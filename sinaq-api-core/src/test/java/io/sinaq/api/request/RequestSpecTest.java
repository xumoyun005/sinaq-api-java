package io.sinaq.api.request;

import io.sinaq.api.SinaqRuntime;
import io.sinaq.api.auth.BearerAuth;
import io.sinaq.api.client.ApiClient;
import io.sinaq.api.client.Sinaq;
import io.sinaq.api.config.RetryPolicy;
import io.sinaq.api.events.EventType;
import io.sinaq.api.events.ReportEvent;
import io.sinaq.api.exception.SinaqConfigurationException;
import io.sinaq.api.exception.SinaqEngineException;
import io.sinaq.api.exception.SinaqException;
import io.sinaq.api.exception.SinaqTimeoutException;
import io.sinaq.api.http.HttpTimeout;
import io.sinaq.api.response.ApiResponse;
import io.sinaq.api.support.StubHttpEngine;
import io.sinaq.api.support.StubHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class RequestSpecTest {

    private final StubHttpEngine engine = new StubHttpEngine();
    private final List<ReportEvent> events = new ArrayList<>();
    private ApiClient api;

    @BeforeEach
    void setUp() {
        SinaqRuntime.reset();
        SinaqRuntime.publisher().register(events::add);
        api = Sinaq.emptyClient()
                .baseUrl("https://api.example.com")
                .defaultHeader("X-App", "1.0")
                .defaultAuth(new BearerAuth("default-token"))
                .engine(engine)
                .build();
        engine.respondJson(200, "{\"ok\":true}");
    }

    @AfterEach
    void tearDown() {
        SinaqRuntime.reset();
    }

    @Test
    void buildsRequestWithParamsHeadersBodyAndAuth() {
        engine.respond(req -> {
            assertThat(req.method().name()).isEqualTo("POST");
            assertThat(req.uri().toString())
                    .isEqualTo("https://api.example.com/users/42?page=1&sort=name");
            assertThat(req.headers().first("Authorization")).contains("Bearer req-token");
            assertThat(req.headers().first("X-App")).contains("1.0");
            assertThat(req.headers().first("X-Custom")).contains("v");
            assertThat(req.headers().first("Content-Type")).contains("application/json");
            assertThat(req.body().orElseThrow().asString()).contains("\"amount\":100");
            assertThat(req.cookies()).hasSize(1);
            return StubHttpResponse.json(200, "{\"ok\":true}");
        });

        api.post("/users/{id}")
                .pathParam("id", 42)
                .queryParam("page", 1)
                .queryParams(Map.of("sort", "name"))
                .header("X-Custom", "v")
                .cookie("sid", "abc")
                .bearer("req-token")
                .body(Map.of("amount", 100))
                .correlationId("corr-1")
                .execute();
    }

    @Test
    void supportsTextFormAndAbsoluteUrl() {
        engine.respond(req -> {
            assertThat(req.uri().toString()).isEqualTo("https://other.example/ping");
            return StubHttpResponse.json(200, "{}");
        });
        api.get("https://other.example/ping").execute();

        engine.respond(req -> {
            assertThat(req.body().orElseThrow().contentType()).contains("text/plain");
            return StubHttpResponse.json(200, "{}");
        });
        api.post("/msg").text("hello").execute();

        engine.respond(req -> {
            assertThat(req.body().orElseThrow().contentType())
                    .contains("application/x-www-form-urlencoded");
            assertThat(req.body().orElseThrow().asString()).contains("a=1");
            return StubHttpResponse.json(200, "{}");
        });
        api.post("/form").form(Map.of("a", 1)).execute();
    }

    @Test
    void stringBodyIsSentAsJson() {
        engine.respond(req -> {
            assertThat(req.body().orElseThrow().asString()).isEqualTo("{\"raw\":true}");
            return StubHttpResponse.json(200, "{}");
        });
        api.post("/x").body("{\"raw\":true}").execute();
    }

    @Test
    void configurationErrors() {
        ApiClient noBase = Sinaq.emptyClient().engine(engine).build();
        assertThatThrownBy(() -> noBase.get("/x").execute())
                .isInstanceOf(SinaqConfigurationException.class)
                .hasMessageContaining("baseUrl");

        assertThatThrownBy(() -> api.get("/users/{id}").execute())
                .isInstanceOf(SinaqConfigurationException.class)
                .hasMessageContaining("Unresolved path parameter");

        assertThatThrownBy(() -> {
            RequestSpec spec = api.get("/once");
            spec.execute();
            spec.execute();
        }).isInstanceOf(SinaqException.class)
                .hasMessageContaining("already executed");
    }

    @Test
    void expectSugarExecutesOnce() {
        ApiResponse r = api.get("/loan")
                .expectStatus(200)
                .expect("ok", true)
                .expectEquals("ok", true)
                .expectNotNull("ok");
        assertThat(r.status()).isEqualTo(200);
        assertThat(events).anyMatch(e -> e.type() == EventType.REQUEST_CREATED);
        assertThat(events).anyMatch(e -> e.type() == EventType.RESPONSE_RECEIVED);
    }

    @Test
    void retryOnStatusCode() {
        AtomicInteger calls = new AtomicInteger();
        engine.respond(req -> {
            int n = calls.incrementAndGet();
            if (n < 3) {
                return StubHttpResponse.json(503, "{}");
            }
            return StubHttpResponse.json(200, "{\"retried\":true}");
        });

        ApiResponse r = api.get("/flaky")
                .retry(RetryPolicy.builder().maxAttempts(5).backoff(Duration.ZERO).onStatus(503).build())
                .execute();
        assertThat(r.status()).isEqualTo(200);
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void retryOnTimeoutAndTransportError() {
        AtomicInteger calls = new AtomicInteger();
        engine.respond(req -> {
            if (calls.incrementAndGet() == 1) {
                throw new SinaqTimeoutException("timeout", null, null);
            }
            return StubHttpResponse.json(200, "{}");
        });
        api.get("/a").retry(RetryPolicy.builder().maxAttempts(2).backoff(Duration.ZERO).onTimeout().build())
                .execute();

        calls.set(0);
        engine.respond(req -> {
            if (calls.incrementAndGet() == 1) {
                throw new SinaqEngineException("boom", null, null);
            }
            return StubHttpResponse.json(200, "{}");
        });
        api.get("/b").retry(RetryPolicy.builder().maxAttempts(2).backoff(Duration.ZERO).onTransportError().build())
                .execute();
    }

    @Test
    void customTimeoutIsForwarded() {
        engine.respond(req -> {
            assertThat(req.timeout()).isEqualTo(HttpTimeout.of(Duration.ofSeconds(1), Duration.ofSeconds(2)));
            return StubHttpResponse.json(200, "{}");
        });
        api.get("/slow").timeout(HttpTimeout.of(Duration.ofSeconds(1), Duration.ofSeconds(2))).execute();
    }

    @Test
    void duplicateHeaderNamesAppend() {
        engine.respond(req -> {
            assertThat(req.headers().all("X-Multi")).containsExactly("one", "two");
            return StubHttpResponse.json(200, "{}");
        });
        api.get("/h").header("X-Multi", "one").header("X-Multi", "two").execute();
    }
}
