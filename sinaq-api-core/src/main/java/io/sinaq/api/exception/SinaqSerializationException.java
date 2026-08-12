package io.sinaq.api.exception;

/** Body serialization or deserialization failure. */
public class SinaqSerializationException extends SinaqException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public SinaqSerializationException(String message) { super(message); }
    public SinaqSerializationException(String message, Throwable cause) { super(message, cause); }
    public SinaqSerializationException(String message, Throwable cause, String correlationId) {
        super(message, cause, correlationId);
    }
}
