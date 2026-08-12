package io.sinaq.api.exception;

/**
 * Transport-level HTTP failure: the request could not be sent or a response
 * could not be obtained/decoded at the transport layer.
 *
 * <p>Hierarchy: {@link SinaqEngineException} (engine adapter failures) and
 * {@link SinaqTimeoutException} (timeouts) are specialisations of this type,
 * so {@code catch (SinaqHttpException e)} covers all transport problems.</p>
 */
public class SinaqHttpException extends SinaqException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public SinaqHttpException(String message) { super(message); }
    public SinaqHttpException(String message, Throwable cause) { super(message, cause); }
    public SinaqHttpException(String message, Throwable cause, String correlationId) {
        super(message, cause, correlationId);
    }
}
