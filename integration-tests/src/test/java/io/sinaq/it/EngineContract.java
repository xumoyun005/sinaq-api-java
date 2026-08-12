package io.sinaq.it;

import io.sinaq.api.client.ApiClient;
import io.sinaq.api.client.Sinaq;
import io.sinaq.api.config.RetryPolicy;
import io.sinaq.api.exception.SinaqAssertionException;
import io.sinaq.api.exception.SinaqTimeoutException;
import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpTimeout;
import io.sinaq.api.response.ApiResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

/**
 * Engine-agnostic contract suite (spec §30): every HttpEngine implementation
 * must pass exactly these tests. Subclasses only supply the engine.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class EngineContract {

    private MockApiServer server;
    private ApiClient api;

    protected abstract HttpEngine engine();

    @BeforeAll
    void startServer() throws Exception {
        server = new MockApiServer();
        api = Sinaq.emptyClient().baseUrl(server.baseUrl()).engine(engine()).build();
    }

    @AfterAll
    void stopServer() {
        if (server != null) {
            server.close();
        }
    }

    protected ApiClient api() {
        return api;
    }

    @Test
    void allVerbsWork() {
        for (String verb : List.of("GET", "POST", "PUT", "PATCH", "DELETE")) {
            ApiResponse r = switch (verb) {
                case "GET" -> api().get("/echo").execute();
                case "POST" -> api().post("/echo").execute();
                case "PUT" -> api().put("/echo").execute();
                case "PATCH" -> api().patch("/echo").execute();
                default -> api().delete("/echo").execute();
            };
            assertThat(r.status()).isEqualTo(200);
            assertThat(r.<String>extract("$.method")).isEqualTo(verb);
        }
    }

    @Test
    void queryParamsBodyAndAuth() {
        ApiResponse r = api().post("/echo")
                .queryParam("page", 1)
                .bearer("tok-1")
                .body(Map.of("amount", 100))
                .execute();
        assertThat(r.<String>extract("$.auth")).isEqualTo("Bearer tok-1");
        assertThat(r.<String>extract("$.query")).contains("page=1");
        assertThat(r.<Long>extract("$.body.amount")).isEqualTo(100L);
    }

    @Test
    void pathParamsAndExtraction() {
        assertThat(api().get("/users/{id}").pathParam("id", 42).execute().<String>extract("$.id"))
                .isEqualTo("42");
        List<Object> uuids = api().get("/cards").execute().extract("$.cards[*].uuid");
        assertThat(uuids).containsExactly("u-1", "u-2");
    }

    @Test
    void fluentAssertionsPassAndFail() {
        api().get("/loan")
                .expectStatus(200)
                .expect("success", true)
                .expectNotNull("loanId")
                .expectResponseTimeLessThan(Duration.ofSeconds(10));
        assertThatThrownBy(() -> api().get("/loan").expectStatus(404))
                .isInstanceOf(SinaqAssertionException.class)
                .hasMessageContaining("expected: 404");
    }

    @Test
    void readTimeoutIsMapped() {
        assertThatThrownBy(() -> api().get("/slow")
                .timeout(HttpTimeout.of(Duration.ofSeconds(2), Duration.ofMillis(200)))
                .execute())
                .isInstanceOf(SinaqTimeoutException.class);
    }

    @Test
    void retryOnConfiguredStatus() {
        ApiResponse r = api().get("/flaky")
                .retry(RetryPolicy.builder().maxAttempts(5).backoff(Duration.ofMillis(10)).onStatus(503).build())
                .execute();
        assertThat(r.status()).isEqualTo(200);
    }

    @Test
    void hundredParallelRequestsNoCrossTalk() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(50);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            final int marker = i;
            futures.add(pool.submit(() -> ("mk-" + marker).equals(
                    api().get("/parallel").queryParam("m", "mk-" + marker).execute().extract("$.marker"))));
        }
        for (Future<Boolean> f : futures) {
            assertThat(f.get(30, TimeUnit.SECONDS)).isTrue();
        }
        pool.shutdown();
    }

    @Test
    void pollingUntilReady() {
        ApiResponse r = api().get("/poll")
                .poll()
                .interval(Duration.ofMillis(10))
                .timeout(Duration.ofSeconds(5))
                .until(resp -> Boolean.TRUE.equals(resp.extract("$.ready")))
                .execute();
        assertThat(r.status()).isEqualTo(200);
    }

    @Test
    void advancedAssertions() {
        api().get("/loan").expectMatches(".*loanId.*");
        api().get("/cards").execute().expectArraySize("$.cards", 2);
    }

    @Test
    void graphqlQuery() {
        api().post("/graphql")
                .graphql("{ hello }")
                .expectStatus(200)
                .expect("data.hello", "world");
    }

    @Test
    void multipartUpload() {
        api().post("/upload")
                .multiPart("name", "demo")
                .multiPartFile("file", "data".getBytes(), "demo.txt")
                .expectStatus(200)
                .expect("multipart", true)
                .expect("hasField", true);
    }

    @Test
    void fiveHundredParallelRequestsNoCrossTalk() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(64);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            final int marker = i;
            futures.add(pool.submit(() -> ("mk-" + marker).equals(
                    api().get("/parallel").queryParam("m", "mk-" + marker).execute().extract("$.marker"))));
        }
        for (Future<Boolean> f : futures) {
            assertThat(f.get(90, TimeUnit.SECONDS)).isTrue();
        }
        pool.shutdown();
    }
}
