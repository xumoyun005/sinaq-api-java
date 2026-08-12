package io.sinaq.api.metrics;

import java.time.Duration;
import java.util.Map;

/**
 * Per-request performance snapshot (V2). Published via {@code PERFORMANCE_RECORDED} events.
 */
public record RequestMetrics(
        String requestId,
        Duration totalTime,
        Duration connectTime,
        Duration readTime,
        int attemptCount,
        long bodyBytesSent,
        long bodyBytesReceived,
        String engine) {

    public Map<String, Object> asPayload() {
        return Map.of(
                "requestId", requestId,
                "totalTimeMs", totalTime.toMillis(),
                "connectTimeMs", connectTime.toMillis(),
                "readTimeMs", readTime.toMillis(),
                "attemptCount", attemptCount,
                "bodyBytesSent", bodyBytesSent,
                "bodyBytesReceived", bodyBytesReceived,
                "engine", engine);
    }
}
