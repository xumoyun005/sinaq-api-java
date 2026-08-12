package io.sinaq.api.exception;

import java.util.Optional;

/**
 * Base runtime exception of the Sinaq framework.
 *
 * <p>Every Sinaq-specific failure extends this type, so user code can catch
 * {@code SinaqException} to handle any framework error. A correlation id is
 * carried when the failure happened in the context of a specific request.</p>
 *
 * <p>Thread-safety: immutable after construction.</p>
 */
public class SinaqException extends RuntimeException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final String correlationId;

    public SinaqException(String message) {
        this(message, null, null);
    }

    public SinaqException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public SinaqException(String message, Throwable cause, String correlationId) {
        super(message, cause);
        this.correlationId = correlationId;
    }

    /** Correlation id of the request during which this failure occurred, if any. */
    public Optional<String> correlationId() {
        return Optional.ofNullable(correlationId);
    }

    @Override
    public String getMessage() {
        String base = super.getMessage();
        return correlationId == null ? base : base + " [correlationId=" + correlationId + "]";
    }
}
