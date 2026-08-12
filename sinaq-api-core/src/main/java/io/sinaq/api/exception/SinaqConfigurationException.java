package io.sinaq.api.exception;

/** Invalid or missing framework configuration (bad base URL, unsupported option, etc.). */
public class SinaqConfigurationException extends SinaqException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public SinaqConfigurationException(String message) { super(message); }
    public SinaqConfigurationException(String message, Throwable cause) { super(message, cause); }
    public SinaqConfigurationException(String message, Throwable cause, String correlationId) {
        super(message, cause, correlationId);
    }
}
