package io.sinaq.api;

import io.sinaq.api.context.ExecutionContext;
import io.sinaq.api.events.EventPublisher;

import java.util.Objects;

/**
 * Process-wide runtime wiring: the shared {@link EventPublisher} and the
 * {@link ExecutionContext} of the current run.
 *
 * <p>This is the framework's single, documented piece of mutable static state.
 * Fields are volatile and safely published; they are set once at startup
 * (typically by a test-framework adapter or reporter) and read by many threads.
 * {@link #reset()} exists for tests only.</p>
 */
public final class SinaqRuntime {

    private static volatile EventPublisher publisher = EventPublisher.createSync();
    private static volatile ExecutionContext executionContext =
            ExecutionContext.create(io.sinaq.api.config.SinaqConfig.DEFAULT_ENVIRONMENT);

    private SinaqRuntime() {}

    public static EventPublisher publisher() {
        return publisher;
    }

    public static ExecutionContext executionContext() {
        return executionContext;
    }

    public static void setPublisher(EventPublisher newPublisher) {
        publisher = Objects.requireNonNull(newPublisher, "publisher");
    }

    public static void setExecutionContext(ExecutionContext context) {
        executionContext = Objects.requireNonNull(context, "context");
    }

    /** Restores defaults. Intended for framework tests. */
    public static void reset() {
        publisher = EventPublisher.createSync();
        executionContext = ExecutionContext.create(io.sinaq.api.config.SinaqConfig.DEFAULT_ENVIRONMENT);
    }
}
