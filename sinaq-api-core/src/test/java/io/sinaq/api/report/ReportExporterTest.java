package io.sinaq.api.report;

import io.sinaq.api.events.EventType;
import io.sinaq.api.events.ReportEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ReportExporterTest {

    @Test
    void exportsEventsJson() {
        ReportEvent e = ReportEvent.builder(EventType.REQUEST_CREATED, "exec-1")
                .requestId("r1")
                .payload(Map.of("method", "GET"))
                .build();
        String json = ReportExporter.toJson(List.of(e));
        assertThat(json).contains("\"schemaVersion\":\"0.4\"");
        assertThat(json).contains("REQUEST_CREATED");
    }
}
