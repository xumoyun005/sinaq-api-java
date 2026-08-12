package io.sinaq.api.http;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Raw transport result produced by an {@link HttpEngine}.
 *
 * <p>This is the engine-facing model: no assertions, no deserialization, no
 * JSONPath — those belong to the core's {@code ApiResponse} built on top of it.
 * No engine-native types (e.g. RestAssured classes) may appear here.</p>
 *
 * <p>Implementations must be immutable and thread-safe;
 * {@link #rawBody()} must return a defensive copy.</p>
 */
public interface HttpResponse {

    /** Numeric status code, e.g. 200. */
    int statusCode();

    /** Status as a value object ({@code HttpStatus.of(statusCode())} by default). */
    default HttpStatus status() {
        return HttpStatus.of(statusCode());
    }

    /** Reason phrase if the engine exposes one. */
    Optional<String> statusText();

    HttpHeaders headers();

    /** Cookies set by the server. Never null; may be empty. Unmodifiable. */
    List<HttpCookie> cookies();

    /** Copy of the raw response body bytes; empty array for no body. */
    byte[] rawBody();

    /** Wall-clock time from sending the request to receiving the full response. */
    Duration responseTime();

    /** Engine-specific diagnostic metadata (string keys). Never null; unmodifiable. */
    Map<String, Object> engineMetadata();
}
