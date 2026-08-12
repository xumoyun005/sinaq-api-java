package io.sinaq.api.response;

import io.sinaq.api.SinaqRuntime;
import io.sinaq.api.client.ApiClient;
import io.sinaq.api.client.Sinaq;
import io.sinaq.api.events.EventType;
import io.sinaq.api.events.ReportEvent;
import io.sinaq.api.exception.SinaqAssertionException;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.support.StubHttpEngine;
import io.sinaq.api.support.StubHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ApiResponseTest {

    private final StubHttpEngine engine = new StubHttpEngine();
    private final ApiClient api = Sinaq.emptyClient()
            .baseUrl("https://api.example.com")
            .engine(engine)
            .build();
    private final List<ReportEvent> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        SinaqRuntime.reset();
        SinaqRuntime.publisher().register(events::add);
        engine.respondJson(200, """
                {"id":42,"name":"Ali","cards":[{"uuid":"u-1"}],"success":true}
                """);
    }

    @AfterEach
    void tearDown() {
        SinaqRuntime.reset();
    }

    @Test
    void accessorsAndExtraction() {
        engine.respond(req -> StubHttpResponse.of(
                201,
                "{\"id\":7,\"cards\":[{\"uuid\":\"u-1\"}]}",
                HttpHeaders.builder().set("X-Trace", "t-1").build(),
                Duration.ofMillis(12)));

        ApiResponse r = api.post("/users").execute();

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.statusValue().code()).isEqualTo(201);
        assertThat(r.statusText()).contains("OK");
        assertThat(r.header("X-Trace")).contains("t-1");
        assertThat(r.headers()).containsKey("X-Trace");
        assertThat(r.text()).contains("\"id\":7");
        assertThat(r.rawBody()).isNotEmpty();
        assertThat(r.responseTime()).isEqualTo(Duration.ofMillis(12));
        assertThat(r.request().method().name()).isEqualTo("POST");
        assertThat(r.<Long>extract("$.id")).isEqualTo(7L);
        List<String> uuids = r.extract("$.cards[*].uuid");
        assertThat(uuids).containsExactly("u-1");
        @SuppressWarnings("unchecked")
        Map<String, Object> map = r.as(Map.class);
        assertThat(map).containsEntry("id", 7L);
        assertThat(r.jsonPath()).isInstanceOf(Map.class);
    }

    @Test
    void assertionsPass() {
        api.get("/users")
                .execute()
                .expectStatus(200)
                .expect("$.success", true)
                .expectEquals("$.id", 42L)
                .expectNotNull("$.name")
                .expectNull("$.missing")
                .expectContains("\"Ali\"")
                .expectResponseTimeLessThan(Duration.ofSeconds(1));

        assertThat(events.stream().map(ReportEvent::type).filter(t -> t == EventType.ASSERTION_PASSED).count())
                .isGreaterThanOrEqualTo(6);
    }

    @Test
    void assertionsFailWithContext() {
        ApiResponse r = api.get("/users").execute();
        assertThatThrownBy(() -> r.expectStatus(404))
                .isInstanceOf(SinaqAssertionException.class)
                .hasMessageContaining("expected: 404")
                .hasMessageContaining("actual: 200");
        assertThatThrownBy(() -> r.expect("$.id", 99))
                .isInstanceOf(SinaqAssertionException.class);
        assertThatThrownBy(() -> r.expectNotNull("$.missing"))
                .isInstanceOf(SinaqAssertionException.class);
        assertThatThrownBy(() -> r.expectNull("$.name"))
                .isInstanceOf(SinaqAssertionException.class);
        assertThatThrownBy(() -> r.expectContains("nope"))
                .isInstanceOf(SinaqAssertionException.class);
        assertThatThrownBy(() -> r.expectResponseTimeLessThan(Duration.ZERO))
                .isInstanceOf(SinaqAssertionException.class);
    }
}
