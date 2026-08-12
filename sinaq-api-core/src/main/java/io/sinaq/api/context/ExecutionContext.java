package io.sinaq.api.context;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Identifies one test run (one JVM execution / CI job). Immutable; thread-safe.
 */
public record ExecutionContext(String executionId, String environment, Instant startedAt) {

    public ExecutionContext {
        Objects.requireNonNull(executionId, "executionId");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(startedAt, "startedAt");
    }

    /** New execution context with a random id, started now. */
    public static ExecutionContext create(String environment) {
        return new ExecutionContext(UUID.randomUUID().toString(), environment, Instant.now());
    }
}
