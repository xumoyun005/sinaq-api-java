package io.sinaq.api.request;

import io.sinaq.api.context.RequestContext;
import io.sinaq.api.http.HttpBody;
import io.sinaq.api.http.HttpCookie;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpMethod;
import io.sinaq.api.http.HttpRequest;
import io.sinaq.api.http.HttpTimeout;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, fully resolved request snapshot handed to the engine.
 * Created by {@link RequestSpec#execute()}. Thread-safe.
 */
public record ApiRequest(HttpMethod method,
                         URI uri,
                         HttpHeaders headers,
                         List<HttpCookie> cookies,
                         HttpBody bodyOrNull,
                         HttpTimeout timeout,
                         RequestContext context) implements HttpRequest {

    public ApiRequest {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(headers, "headers");
        cookies = cookies == null ? List.of() : List.copyOf(cookies);
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(context, "context");
    }

    @Override
    public Optional<HttpBody> body() {
        return Optional.ofNullable(bodyOrNull);
    }
}
