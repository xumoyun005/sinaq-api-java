package io.sinaq.jdk;

import io.sinaq.api.exception.SinaqEngineException;
import io.sinaq.api.exception.SinaqTimeoutException;
import io.sinaq.api.http.HttpBody;
import io.sinaq.api.http.HttpCookie;
import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpRequest;
import io.sinaq.api.http.HttpResponse;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link HttpEngine} on top of {@code java.net.http} — zero external dependencies.
 *
 * <p>Thread-safe: JDK HttpClient instances are immutable and shared; one client
 * is cached per connect-timeout value (the JDK sets connect timeout at client
 * level, read timeout at request level). No global/static JDK state is touched.</p>
 */
public final class JdkHttpEngine implements HttpEngine {

    private final ConcurrentHashMap<Duration, HttpClient> clients = new ConcurrentHashMap<>();

    @Override
    public HttpResponse execute(HttpRequest request) {
        HttpClient client = clients.computeIfAbsent(request.timeout().connect(),
                connect -> HttpClient.newBuilder()
                        .connectTimeout(connect)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build());

        java.net.http.HttpRequest.Builder b = java.net.http.HttpRequest.newBuilder()
                .uri(request.uri())
                .timeout(request.timeout().read());

        request.headers().asMap().forEach((name, values) ->
                values.forEach(v -> b.header(name, v)));
        if (!request.cookies().isEmpty()) {
            StringBuilder cookie = new StringBuilder();
            for (HttpCookie c : request.cookies()) {
                if (!cookie.isEmpty()) cookie.append("; ");
                cookie.append(c.name()).append('=').append(c.value());
            }
            b.header("Cookie", cookie.toString());
        }

        java.net.http.HttpRequest.BodyPublisher publisher = request.body()
                .map(body -> java.net.http.HttpRequest.BodyPublishers.ofByteArray(body.bytes()))
                .orElse(java.net.http.HttpRequest.BodyPublishers.noBody());
        b.method(request.method().name(), publisher);
        if (request.body().isPresent() && request.headers().first("Content-Type").isEmpty()) {
            request.body().flatMap(HttpBody::contentType)
                    .ifPresent(ct -> b.header("Content-Type", ct));
        }

        String correlationId = request.context().correlationId().orElse(null);
        Instant start = Instant.now();
        try {
            java.net.http.HttpResponse<byte[]> raw =
                    client.send(b.build(), java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            Duration elapsed = Duration.between(start, Instant.now());
            return new JdkResponse(raw, elapsed);
        } catch (HttpConnectTimeoutException e) {
            throw new SinaqTimeoutException("Connect timeout after " + request.timeout().connect()
                    + " for " + request.method() + " " + request.uri(), e, correlationId);
        } catch (HttpTimeoutException e) {
            throw new SinaqTimeoutException("Read timeout after " + request.timeout().read()
                    + " for " + request.method() + " " + request.uri(), e, correlationId);
        } catch (IOException e) {
            throw new SinaqEngineException("Transport failure for " + request.method() + " "
                    + request.uri() + ": " + e.getMessage(), e, correlationId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SinaqEngineException("Interrupted while executing " + request.uri(), e, correlationId);
        }
    }

    @Override
    public String name() {
        return "jdk";
    }

    /** Immutable translation of the JDK response into the Sinaq model. */
    private static final class JdkResponse implements HttpResponse {

        private final int statusCode;
        private final HttpHeaders headers;
        private final List<HttpCookie> cookies;
        private final byte[] body;
        private final Duration responseTime;
        private final Map<String, Object> metadata;

        private JdkResponse(java.net.http.HttpResponse<byte[]> raw, Duration responseTime) {
            this.statusCode = raw.statusCode();
            HttpHeaders.Builder hb = HttpHeaders.builder();
            raw.headers().map().forEach((name, values) -> values.forEach(v -> hb.add(name, v)));
            this.headers = hb.build();
            this.cookies = parseCookies(raw.headers().allValues("Set-Cookie"));
            byte[] b = raw.body();
            this.body = b == null ? new byte[0] : b;
            this.responseTime = responseTime;
            this.metadata = Map.of("httpVersion", raw.version().name(), "engine", "jdk");
        }

        private static List<HttpCookie> parseCookies(List<String> setCookieHeaders) {
            List<HttpCookie> out = new ArrayList<>();
            for (String header : setCookieHeaders) {
                String firstPair = header.split(";", 2)[0];
                int eq = firstPair.indexOf('=');
                if (eq > 0) {
                    out.add(HttpCookie.of(firstPair.substring(0, eq).trim(),
                                          firstPair.substring(eq + 1).trim()));
                }
            }
            return List.copyOf(out);
        }

        @Override public int statusCode()                     { return statusCode; }
        @Override public Optional<String> statusText()        { return Optional.empty(); }
        @Override public HttpHeaders headers()                { return headers; }
        @Override public List<HttpCookie> cookies()           { return cookies; }
        @Override public byte[] rawBody()                     { return body.clone(); }
        @Override public Duration responseTime()              { return responseTime; }
        @Override public Map<String, Object> engineMetadata() { return metadata; }
    }
}
