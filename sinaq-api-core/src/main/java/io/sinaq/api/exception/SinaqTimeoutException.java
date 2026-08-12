package io.sinaq.api.exception;

/** Connect or read timeout while executing a request. */
public class SinaqTimeoutException extends SinaqEngineException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public SinaqTimeoutException(String message) { super(message); }
    public SinaqTimeoutException(String message, Throwable cause) { super(message, cause); }
    public SinaqTimeoutException(String message, Throwable cause, String correlationId) {
        super(message, cause, correlationId);
    }
}
