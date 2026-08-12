package io.sinaq.api.recording;

import io.sinaq.api.SinaqRuntime;
import io.sinaq.api.events.EventType;
import io.sinaq.api.events.ReportEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ExchangeRecorderTest {

    private final ExchangeRecorder recorder = new ExchangeRecorder();

    @AfterEach
    void tearDown() {
        recorder.clear();
    }

    @Test
    void recordsRequestResponsePair() {
        SinaqRuntime.publisher().register(recorder);
        try {
            ReportEvent created = ReportEvent.builder(EventType.REQUEST_CREATED, "exec-1")
                    .requestId("r1")
                    .payload(Map.of(
                            "method", "GET",
                            "url", "http://localhost/x",
                            "headers", Map.of("X-Test", "1"),
                            "body", "{\"a\":1}"))
                    .build();
            ReportEvent received = ReportEvent.builder(EventType.RESPONSE_RECEIVED, "exec-1")
                    .requestId("r1")
                    .payload(Map.of(
                            "status", 200,
                            "responseTimeMs", 12L,
                            "headers", Map.of("Content-Type", "application/json"),
                            "body", "{\"ok\":true}"))
                    .build();
            SinaqRuntime.publisher().publish(created);
            SinaqRuntime.publisher().publish(received);
            assertThat(recorder.exchanges()).hasSize(1);
            RecordedExchange ex = recorder.exchanges().get(0);
            assertThat(ex.method()).isEqualTo("GET");
            assertThat(ex.status()).isEqualTo(200);
            assertThat(ex.responseBody()).contains("ok");
        } finally {
            SinaqRuntime.publisher().unregister(recorder);
        }
    }
}
