package io.sinaq.api.har;

import io.sinaq.api.recording.RecordedExchange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class HarExporterTest {

    @Test
    void exportsHarJson() {
        RecordedExchange ex = new RecordedExchange(
                "r1", "GET", "http://localhost/x", Map.of(), "",
                200, Map.of(), "{\"ok\":true}", 10, Instant.parse("2026-01-01T00:00:00Z"));
        String har = HarExporter.toHarJson(List.of(ex));
        assertThat(har).contains("\"version\":\"1.2\"");
        assertThat(har).contains("http://localhost/x");
        assertThat(har).contains("ok");
    }
}
