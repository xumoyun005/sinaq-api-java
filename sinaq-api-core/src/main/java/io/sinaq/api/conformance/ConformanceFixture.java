package io.sinaq.api.conformance;

import io.sinaq.api.internal.json.JsonWriter;

import java.util.List;
import java.util.Map;

/**
 * Cross-language conformance fixture export (V4).
 */
public final class ConformanceFixture {

    private ConformanceFixture() {}

    public static String exportSuite() {
        return JsonWriter.write(Map.of(
                "schemaVersion", "0.4",
                "language", "java",
                "cases", List.of(
                        Map.of("name", "getWithStatus", "method", "GET", "path", "/health",
                                "expectStatus", 200),
                        Map.of("name", "postWithBody", "method", "POST", "path", "/echo",
                                "body", Map.of("key", "value"), "expectStatus", 200),
                        Map.of("name", "retryOn503", "method", "GET", "path", "/flaky",
                                "retry", true, "expectStatus", 200))));
    }
}
