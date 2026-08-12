package io.sinaq.examples;

import io.sinaq.api.client.ApiClient;
import io.sinaq.api.client.Sinaq;
import io.sinaq.api.events.EventType;
import io.sinaq.api.events.ReportEvent;
import io.sinaq.api.response.ApiResponse;
import io.sinaq.jdk.JdkHttpEngine;
import io.sinaq.junit5.SinaqExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Minimal runnable example: JDK engine, fluent DSL, assertions, extraction, events.
 */
@ExtendWith(SinaqExtension.class)
class QuickStartExample {

    private static final List<ReportEvent> EVENTS = new CopyOnWriteArrayList<>();
    private static ExampleServer server;
    private static ApiClient api;

    @BeforeAll
    static void setUp() throws Exception {
        io.sinaq.api.SinaqRuntime.publisher().register(EVENTS::add);
        server = new ExampleServer();
        api = Sinaq.emptyClient()
                .baseUrl(server.baseUrl())
                .engine(new JdkHttpEngine())
                .defaultHeader("X-Demo", "sinaq-examples")
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void getWithQueryParam() {
        ApiResponse r = api.get("/get").queryParam("page", 1).execute();
        r.expectStatus(200).expect("page", "1");
    }

    @Test
    void postWithBearerAndBody() {
        api.post("/post")
                .bearer("demo-token")
                .body(Map.of("amount", 150, "currency", "UZS"))
                .expectStatus(200)
                .expectContains("demo-token");
    }

    @Test
    void extractFromJson() {
        String origin = api.get("/get").execute().extract("$.origin");
        assertThat(origin).isEqualTo("127.0.0.1");
        assertThat(EVENTS.stream().anyMatch(e -> e.type() == EventType.REQUEST_CREATED)).isTrue();
    }
}
