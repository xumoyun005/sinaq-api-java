package io.sinaq.api.exception;

/**
 * A Sinaq assertion failed ({@code expectStatus}, {@code expect}, ...).
 *
 * <p>The assertion engine is test-framework independent: it always throws this
 * exception; TestNG/JUnit adapters translate it into a test failure.</p>
 */
public class SinaqAssertionException extends SinaqException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public SinaqAssertionException(String message) { super(message); }
    public SinaqAssertionException(String message, Throwable cause) { super(message, cause); }
    public SinaqAssertionException(String message, Throwable cause, String correlationId) {
        super(message, cause, correlationId);
    }
}
