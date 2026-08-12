package io.sinaq.api.config;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * Explicit, opt-in retry for transport/transient conditions only (spec §20).
 * Assertion failures never reach the retry path by construction — retry wraps
 * engine execution, assertions run afterwards. Immutable; thread-safe.
 */
public record RetryPolicy(int maxAttempts,
                          Duration backoff,
                          boolean onTimeout,
                          boolean onTransportError,
                          Set<Integer> onStatusCodes) {

    private static final RetryPolicy NONE =
            new RetryPolicy(1, Duration.ZERO, false, false, Set.of());

    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1: " + maxAttempts);
        }
        Objects.requireNonNull(backoff, "backoff");
        if (backoff.isNegative()) {
            throw new IllegalArgumentException("backoff must not be negative: " + backoff);
        }
        onStatusCodes = Set.copyOf(onStatusCodes);
    }

    /** No retry — the framework default. */
    public static RetryPolicy none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean enabled() {
        return maxAttempts > 1;
    }

    /** Mutable builder. NOT thread-safe; confine to one thread. */
    public static final class Builder {
        private int maxAttempts = 3;
        private Duration backoff = Duration.ofMillis(200);
        private boolean onTimeout;
        private boolean onTransportError;
        private Set<Integer> statusCodes = Set.of();

        private Builder() {}

        public Builder maxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; return this; }
        public Builder backoff(Duration backoff)    { this.backoff = backoff; return this; }
        public Builder onTimeout()                  { this.onTimeout = true; return this; }
        public Builder onTransportError()           { this.onTransportError = true; return this; }
        public Builder onStatus(Integer... codes)   { this.statusCodes = Set.of(codes); return this; }

        public RetryPolicy build() {
            return new RetryPolicy(maxAttempts, backoff, onTimeout, onTransportError, statusCodes);
        }
    }
}
