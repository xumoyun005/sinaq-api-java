package io.sinaq.api.http;

import java.time.Duration;
import java.util.Objects;

/**
 * Connect/read timeout pair. Immutable; thread-safe.
 * Engines that cannot honor a configured timeout must fail fast with
 * {@code SinaqConfigurationException} instead of silently ignoring it.
 */
public record HttpTimeout(Duration connect, Duration read) {

    private static final HttpTimeout DEFAULTS =
            new HttpTimeout(Duration.ofSeconds(10), Duration.ofSeconds(30));

    public HttpTimeout {
        requirePositive(connect, "connect");
        requirePositive(read, "read");
    }

    /** Framework defaults: connect 10s, read 30s. */
    public static HttpTimeout defaults() {
        return DEFAULTS;
    }

    public static HttpTimeout of(Duration connect, Duration read) {
        return new HttpTimeout(connect, read);
    }

    private static void requirePositive(Duration d, String label) {
        Objects.requireNonNull(d, label + " timeout");
        if (d.isZero() || d.isNegative()) {
            throw new IllegalArgumentException(label + " timeout must be positive: " + d);
        }
    }
}
