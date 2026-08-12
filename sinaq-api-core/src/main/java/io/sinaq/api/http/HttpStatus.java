package io.sinaq.api.http;

/**
 * HTTP status code value object with class helpers.
 * Immutable; thread-safe.
 */
public record HttpStatus(int code) {

    public HttpStatus {
        if (code < 100 || code > 599) {
            throw new IllegalArgumentException("HTTP status code out of range (100..599): " + code);
        }
    }

    public static HttpStatus of(int code) {
        return new HttpStatus(code);
    }

    public boolean isInformational() { return code >= 100 && code < 200; }
    public boolean isSuccess()       { return code >= 200 && code < 300; }
    public boolean isRedirect()      { return code >= 300 && code < 400; }
    public boolean isClientError()   { return code >= 400 && code < 500; }
    public boolean isServerError()   { return code >= 500 && code < 600; }
    public boolean isError()         { return isClientError() || isServerError(); }

    @Override
    public String toString() {
        return String.valueOf(code);
    }
}
