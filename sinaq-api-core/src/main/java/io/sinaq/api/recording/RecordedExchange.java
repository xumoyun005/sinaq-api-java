package io.sinaq.api.recording;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Immutable HTTP exchange captured for contract testing / HAR (V3). */
public record RecordedExchange(
        String requestId,
        String method,
        String url,
        Map<String, String> requestHeaders,
        String requestBody,
        int status,
        Map<String, String> responseHeaders,
        String responseBody,
        long durationMs,
        Instant timestamp) {

    public RecordedExchange {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(url, "url");
        requestHeaders = requestHeaders == null ? Map.of() : Map.copyOf(requestHeaders);
        requestBody = requestBody == null ? "" : requestBody;
        responseHeaders = responseHeaders == null ? Map.of() : Map.copyOf(responseHeaders);
        responseBody = responseBody == null ? "" : responseBody;
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }
}
