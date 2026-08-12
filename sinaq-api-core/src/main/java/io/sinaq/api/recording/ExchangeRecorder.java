package io.sinaq.api.recording;

import io.sinaq.api.events.EventListener;
import io.sinaq.api.events.EventType;
import io.sinaq.api.events.ReportEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records request/response pairs from framework events (V3).
 */
public final class ExchangeRecorder implements EventListener {

    private final List<RecordedExchange> exchanges = new CopyOnWriteArrayList<>();
    private final Map<String, Pending> pending = new HashMap<>();

    @Override
    public void onEvent(ReportEvent event) {
        if (event.type() == EventType.REQUEST_CREATED) {
            String requestId = event.requestId().orElse(null);
            if (requestId == null) {
                return;
            }
            Map<String, Object> p = event.payload();
            pending.put(requestId, new Pending(
                    String.valueOf(p.getOrDefault("method", "")),
                    String.valueOf(p.getOrDefault("url", "")),
                    flattenHeaders(p.get("headers")),
                    String.valueOf(p.getOrDefault("body", "")),
                    event.timestamp()));
        } else if (event.type() == EventType.RESPONSE_RECEIVED) {
            String requestId = event.requestId().orElse(null);
            if (requestId == null) {
                return;
            }
            Pending req = pending.remove(requestId);
            if (req == null) {
                return;
            }
            Map<String, Object> p = event.payload();
            exchanges.add(new RecordedExchange(
                    requestId,
                    req.method,
                    req.url,
                    req.headers,
                    req.body,
                    ((Number) p.getOrDefault("status", 0)).intValue(),
                    flattenHeaders(p.get("headers")),
                    String.valueOf(p.getOrDefault("body", "")),
                    ((Number) p.getOrDefault("responseTimeMs", 0L)).longValue(),
                    event.timestamp()));
        }
    }

    public List<RecordedExchange> exchanges() {
        return List.copyOf(exchanges);
    }

    public void clear() {
        exchanges.clear();
        pending.clear();
    }

    private static Map<String, String> flattenHeaders(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            Map<String, String> out = new HashMap<>();
            map.forEach((k, v) -> {
                if (v instanceof List<?> list) {
                    out.put(String.valueOf(k), String.join(",", list.stream()
                            .map(String::valueOf).toList()));
                } else {
                    out.put(String.valueOf(k), String.valueOf(v));
                }
            });
            return out;
        }
        return Map.of();
    }

    private record Pending(String method, String url, Map<String, String> headers,
                           String body, Instant timestamp) {}
}
