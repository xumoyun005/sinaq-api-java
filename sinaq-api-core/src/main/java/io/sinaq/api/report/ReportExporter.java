package io.sinaq.api.report;

import io.sinaq.api.events.ReportEvent;
import io.sinaq.api.internal.json.JsonWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Exports collected report events to JSON (V4 richer payloads).
 */
public final class ReportExporter {

    private ReportExporter() {}

    public static String toJson(List<ReportEvent> events) {
        Objects.requireNonNull(events, "events");
        List<Map<String, Object>> items = new ArrayList<>();
        for (ReportEvent e : events) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", e.type().name());
            item.put("timestamp", e.timestamp().toString());
            item.put("executionId", e.executionId());
            e.testId().ifPresent(v -> item.put("testId", v));
            e.requestId().ifPresent(v -> item.put("requestId", v));
            e.correlationId().ifPresent(v -> item.put("correlationId", v));
            item.put("payload", e.payload());
            items.add(item);
        }
        return JsonWriter.write(Map.of(
                "schemaVersion", "0.4",
                "eventCount", items.size(),
                "events", items));
    }
}
