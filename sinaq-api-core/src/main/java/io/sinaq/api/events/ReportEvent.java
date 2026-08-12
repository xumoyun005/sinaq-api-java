package io.sinaq.api.events;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One immutable report event. This shape is the shared report schema envelope
 * for all Sinaq SDKs (Java/Python/.NET).
 *
 * <p>CONTRACT: payload values must already be masked when the event is built —
 * see {@code io.sinaq.api.masking.Masker}. Thread-safe.</p>
 */
public final class ReportEvent {

    private final EventType type;
    private final Instant timestamp;
    private final String executionId;
    private final String testId;         // nullable
    private final String requestId;      // nullable
    private final String correlationId;  // nullable
    private final Map<String, Object> payload;

    private ReportEvent(Builder b) {
        this.type = b.type;
        this.timestamp = b.timestamp;
        this.executionId = b.executionId;
        this.testId = b.testId;
        this.requestId = b.requestId;
        this.correlationId = b.correlationId;
        this.payload = Map.copyOf(b.payload);
    }

    public EventType type()                 { return type; }
    public Instant timestamp()              { return timestamp; }
    public String executionId()             { return executionId; }
    public Optional<String> testId()        { return Optional.ofNullable(testId); }
    public Optional<String> requestId()     { return Optional.ofNullable(requestId); }
    public Optional<String> correlationId() { return Optional.ofNullable(correlationId); }
    public Map<String, Object> payload()    { return payload; }

    public static Builder builder(EventType type, String executionId) {
        return new Builder(type, executionId);
    }

    @Override
    public String toString() {
        return "ReportEvent[" + type + " exec=" + executionId
                + (requestId != null ? " req=" + requestId : "") + "]";
    }

    /** Mutable builder. NOT thread-safe; confine to one thread. */
    public static final class Builder {

        private final EventType type;
        private final String executionId;
        private Instant timestamp = Instant.now();
        private String testId;
        private String requestId;
        private String correlationId;
        private Map<String, Object> payload = Map.of();

        private Builder(EventType type, String executionId) {
            this.type = Objects.requireNonNull(type, "type");
            this.executionId = Objects.requireNonNull(executionId, "executionId");
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
            return this;
        }

        public Builder testId(String testId)               { this.testId = testId; return this; }
        public Builder requestId(String requestId)         { this.requestId = requestId; return this; }
        public Builder correlationId(String correlationId) { this.correlationId = correlationId; return this; }

        /** Payload must contain ALREADY MASKED values only. */
        public Builder payload(Map<String, Object> payload) {
            this.payload = Objects.requireNonNull(payload, "payload");
            return this;
        }

        public ReportEvent build() {
            return new ReportEvent(this);
        }
    }
}
