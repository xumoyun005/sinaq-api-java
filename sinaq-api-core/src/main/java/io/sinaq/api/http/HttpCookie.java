package io.sinaq.api.http;

import java.util.Objects;

/**
 * Immutable HTTP cookie. {@code domain} and {@code path} may be null.
 * Thread-safe.
 */
public record HttpCookie(String name,
                         String value,
                         String domain,
                         String path,
                         boolean secure,
                         boolean httpOnly) {

    public HttpCookie {
        Objects.requireNonNull(name, "cookie name");
        Objects.requireNonNull(value, "cookie value");
        if (name.isBlank()) {
            throw new IllegalArgumentException("cookie name must not be blank");
        }
    }

    /** Simple cookie with only name and value. */
    public static HttpCookie of(String name, String value) {
        return new HttpCookie(name, value, null, null, false, false);
    }
}
