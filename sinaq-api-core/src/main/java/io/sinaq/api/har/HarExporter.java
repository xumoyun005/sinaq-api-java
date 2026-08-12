package io.sinaq.api.har;

import io.sinaq.api.internal.json.JsonWriter;
import io.sinaq.api.recording.RecordedExchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Exports recorded exchanges to HAR 1.2 JSON (V3). */
public final class HarExporter {

    private HarExporter() {}

    public static String toHarJson(List<RecordedExchange> exchanges) {
        Objects.requireNonNull(exchanges, "exchanges");
        List<Map<String, Object>> entries = new ArrayList<>();
        for (RecordedExchange ex : exchanges) {
            entries.add(entry(ex));
        }
        Map<String, Object> log = new HashMap<>();
        log.put("version", "1.2");
        log.put("creator", Map.of("name", "Sinaq API", "version", "0.3"));
        log.put("entries", entries);
        return JsonWriter.write(Map.of("log", log));
    }

    private static Map<String, Object> entry(RecordedExchange ex) {
        Map<String, Object> e = new HashMap<>();
        e.put("startedDateTime", ex.timestamp().toString());
        e.put("time", ex.durationMs());
        e.put("request", request(ex));
        e.put("response", response(ex));
        e.put("cache", Map.of());
        e.put("timings", Map.of("send", 0, "wait", ex.durationMs(), "receive", 0));
        return e;
    }

    private static Map<String, Object> request(RecordedExchange ex) {
        Map<String, Object> r = new HashMap<>();
        r.put("method", ex.method());
        r.put("url", ex.url());
        r.put("httpVersion", "HTTP/1.1");
        r.put("headers", harHeaders(ex.requestHeaders()));
        r.put("queryString", List.of());
        r.put("postData", Map.of(
                "mimeType", "application/json",
                "text", ex.requestBody()));
        r.put("headersSize", -1);
        r.put("bodySize", ex.requestBody().length());
        return r;
    }

    private static Map<String, Object> response(RecordedExchange ex) {
        Map<String, Object> r = new HashMap<>();
        r.put("status", ex.status());
        r.put("statusText", "");
        r.put("httpVersion", "HTTP/1.1");
        r.put("headers", harHeaders(ex.responseHeaders()));
        r.put("content", Map.of(
                "mimeType", "application/json",
                "text", ex.responseBody(),
                "size", ex.responseBody().length()));
        r.put("headersSize", -1);
        r.put("bodySize", ex.responseBody().length());
        return r;
    }

    private static List<Map<String, String>> harHeaders(Map<String, String> headers) {
        List<Map<String, String>> out = new ArrayList<>();
        headers.forEach((name, value) -> out.add(Map.of("name", name, "value", value)));
        return out;
    }
}
