package io.sinaq.api.http;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable {@link HttpResponse} for interceptors and tests (V3). */
public final class ImmutableHttpResponse implements HttpResponse {

    private final int statusCode;
    private final Optional<String> statusText;
    private final HttpHeaders headers;
    private final List<HttpCookie> cookies;
    private final byte[] body;
    private final Duration responseTime;
    private final Map<String, Object> metadata;

    public ImmutableHttpResponse(int statusCode, HttpHeaders headers, byte[] body,
                                 Duration responseTime) {
        this(statusCode, Optional.empty(), headers, List.of(), body, responseTime, Map.of());
    }

    public ImmutableHttpResponse(int statusCode, Optional<String> statusText, HttpHeaders headers,
                                 List<HttpCookie> cookies, byte[] body, Duration responseTime,
                                 Map<String, Object> metadata) {
        this.statusCode = statusCode;
        this.statusText = statusText == null ? Optional.empty() : statusText;
        this.headers = Objects.requireNonNull(headers, "headers");
        this.cookies = cookies == null ? List.of() : List.copyOf(cookies);
        this.body = body == null ? new byte[0] : Arrays.copyOf(body, body.length);
        this.responseTime = Objects.requireNonNull(responseTime, "responseTime");
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

  public static ImmutableHttpResponse copyOf(HttpResponse source) {
        return new ImmutableHttpResponse(
                source.statusCode(),
                source.statusText(),
                source.headers(),
                source.cookies(),
                source.rawBody(),
                source.responseTime(),
                source.engineMetadata());
    }

    @Override public int statusCode() { return statusCode; }
    @Override public Optional<String> statusText() { return statusText; }
    @Override public HttpHeaders headers() { return headers; }
    @Override public List<HttpCookie> cookies() { return cookies; }
    @Override public byte[] rawBody() { return Arrays.copyOf(body, body.length); }
    @Override public Duration responseTime() { return responseTime; }
    @Override public Map<String, Object> engineMetadata() { return metadata; }
}
