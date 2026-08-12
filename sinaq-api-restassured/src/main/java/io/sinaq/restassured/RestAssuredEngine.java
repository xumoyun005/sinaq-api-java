package io.sinaq.restassured;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.sinaq.api.exception.SinaqEngineException;
import io.sinaq.api.exception.SinaqTimeoutException;
import io.sinaq.api.http.HttpCookie;
import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpRequest;
import io.sinaq.api.http.HttpResponse;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link HttpEngine} over RestAssured 5.x.
 *
 * <p>Builds a FRESH request specification per call and configures timeouts via
 * a per-request {@link RestAssuredConfig} — it never touches RestAssured's
 * mutable static configuration, so parallel execution is safe (R3).
 * No RestAssured type leaks through the SPI.</p>
 */
public final class RestAssuredEngine implements HttpEngine {

    @Override
    public HttpResponse execute(HttpRequest request) {
        String correlationId = request.context().correlationId().orElse(null);
        try {
            RestAssuredConfig config = RestAssuredConfig.config().httpClient(
                    HttpClientConfig.httpClientConfig()
                            .setParam("http.connection.timeout",
                                    (int) request.timeout().connect().toMillis())
                            .setParam("http.socket.timeout",
                                    (int) request.timeout().read().toMillis()));

            RequestSpecification spec = RestAssured.given()
                    .config(config)
                    .urlEncodingEnabled(false);   // core already encoded the URI

            request.headers().asMap().forEach((name, values) ->
                    values.forEach(v -> spec.header(name, v)));
            for (HttpCookie c : request.cookies()) {
                spec.cookie(c.name(), c.value());
            }
            request.body().ifPresent(body -> spec.body(body.bytes()));

            long start = System.nanoTime();
            Response response = spec.request(request.method().name(), request.uri());
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
            return new RestAssuredResponse(response, elapsed);
        } catch (Exception e) {
            if (hasCause(e, SocketTimeoutException.class)) {
                throw new SinaqTimeoutException("Timeout for " + request.method() + " "
                        + request.uri(), e, correlationId);
            }
            throw new SinaqEngineException("Transport failure for " + request.method() + " "
                    + request.uri() + ": " + e.getMessage(), e, correlationId);
        }
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (type.isInstance(c)) return true;
        }
        return false;
    }

    @Override
    public String name() {
        return "restassured";
    }

    private static final class RestAssuredResponse implements HttpResponse {

        private final int statusCode;
        private final String statusLine;
        private final HttpHeaders headers;
        private final List<HttpCookie> cookies;
        private final byte[] body;
        private final Duration responseTime;

        private RestAssuredResponse(Response response, Duration elapsed) {
            this.statusCode = response.getStatusCode();
            this.statusLine = response.getStatusLine();
            HttpHeaders.Builder hb = HttpHeaders.builder();
            response.getHeaders().forEach(h -> hb.add(h.getName(), h.getValue()));
            this.headers = hb.build();
            this.cookies = response.getCookies().entrySet().stream()
                    .map(e -> HttpCookie.of(e.getKey(), e.getValue()))
                    .toList();
            byte[] raw = response.getBody().asByteArray();
            this.body = raw == null ? new byte[0] : raw;
            this.responseTime = elapsed;
        }

        @Override public int statusCode()                     { return statusCode; }
        @Override public Optional<String> statusText()        { return Optional.ofNullable(statusLine); }
        @Override public HttpHeaders headers()                { return headers; }
        @Override public List<HttpCookie> cookies()           { return cookies; }
        @Override public byte[] rawBody()                     { return body.clone(); }
        @Override public Duration responseTime()              { return responseTime; }
        @Override public Map<String, Object> engineMetadata() { return Map.of("engine", "restassured"); }
    }
}
