package io.sinaq.api.http;

import io.sinaq.api.context.RequestContext;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Fully resolved, immutable request handed to an {@link HttpEngine}.
 *
 * <p>By the time an engine sees this object, the core has already resolved the
 * base URL, substituted path parameters, merged default headers, applied
 * authentication and serialized the body. Engines perform transport only.</p>
 *
 * <p>Implementations must be immutable and thread-safe.</p>
 */
public interface HttpRequest {

    HttpMethod method();

    /** Absolute URI including path and encoded query string. */
    URI uri();

    HttpHeaders headers();

    /** Cookies to send. Never null; may be empty. Unmodifiable. */
    List<HttpCookie> cookies();

    /** Request body; empty for bodiless requests such as GET. */
    Optional<HttpBody> body();

    HttpTimeout timeout();

    /** Context identifying this request for events, logs and errors. */
    RequestContext context();
}
