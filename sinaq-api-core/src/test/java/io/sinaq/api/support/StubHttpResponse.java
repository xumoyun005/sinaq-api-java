package io.sinaq.api.support;

import io.sinaq.api.http.HttpCookie;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Test double for {@link HttpResponse}. */
public final class StubHttpResponse implements HttpResponse {

    private final int statusCode;
    private final HttpHeaders headers;
    private final List<HttpCookie> cookies;
    private final byte[] body;
    private final Duration responseTime;

    private StubHttpResponse(int statusCode, HttpHeaders headers, List<HttpCookie> cookies,
                             byte[] body, Duration responseTime) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.cookies = cookies;
        this.body = body;
        this.responseTime = responseTime;
    }

    public static HttpResponse json(int status, String json) {
        return json(status, json, Duration.ofMillis(5));
    }

    public static HttpResponse json(int status, String json, Duration responseTime) {
        return new StubHttpResponse(
                status,
                HttpHeaders.builder().set("Content-Type", "application/json").build(),
                List.of(),
                json.getBytes(StandardCharsets.UTF_8),
                responseTime);
    }

    public static HttpResponse of(int status, String body, HttpHeaders headers, Duration responseTime) {
        return new StubHttpResponse(status, headers, List.of(),
                body.getBytes(StandardCharsets.UTF_8), responseTime);
    }

    @Override public int statusCode()                     { return statusCode; }
    @Override public Optional<String> statusText()        { return Optional.of("OK"); }
    @Override public HttpHeaders headers()                { return headers; }
    @Override public List<HttpCookie> cookies()           { return cookies; }
    @Override public byte[] rawBody()                     { return body.clone(); }
    @Override public Duration responseTime()              { return responseTime; }
    @Override public Map<String, Object> engineMetadata() { return Map.of("engine", "stub"); }
}
