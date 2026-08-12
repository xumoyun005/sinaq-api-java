package io.sinaq.api.exception;

/**
 * Failure raised by an {@link io.sinaq.api.http.HttpEngine} implementation while
 * executing a request (connection refused, TLS failure, engine misbehaviour...).
 */
public class SinaqEngineException extends SinaqHttpException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public SinaqEngineException(String message) { super(message); }
    public SinaqEngineException(String message, Throwable cause) { super(message, cause); }
    public SinaqEngineException(String message, Throwable cause, String correlationId) {
        super(message, cause, correlationId);
    }
}
