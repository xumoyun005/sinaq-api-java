package io.sinaq.okhttp;

import io.sinaq.api.exception.SinaqEngineException;
import io.sinaq.api.exception.SinaqTimeoutException;
import io.sinaq.api.http.HttpBody;
import io.sinaq.api.http.HttpCookie;
import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpRequest;
import io.sinaq.api.http.HttpResponse;
import io.sinaq.api.http.ImmutableHttpResponse;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * {@link HttpEngine} on OkHttp 4.x (V3).
 */
public final class OkHttpEngine implements HttpEngine {

    private final OkHttpClient baseClient;

    public OkHttpEngine() {
        this.baseClient = new OkHttpClient.Builder()
                .followRedirects(false)
                .build();
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
        String correlationId = request.context().correlationId().orElse(null);
        OkHttpClient client = baseClient.newBuilder()
                .connectTimeout(request.timeout().connect().toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(request.timeout().read().toMillis(), TimeUnit.MILLISECONDS)
                .build();

        Request.Builder rb = new Request.Builder().url(request.uri().toString());
        request.headers().asMap().forEach((name, values) ->
                values.forEach(v -> rb.addHeader(name, v)));
        if (!request.cookies().isEmpty()) {
            StringBuilder cookie = new StringBuilder();
            for (HttpCookie c : request.cookies()) {
                if (!cookie.isEmpty()) cookie.append("; ");
                cookie.append(c.name()).append('=').append(c.value());
            }
            rb.addHeader("Cookie", cookie.toString());
        }

        RequestBody requestBody = request.body()
                .map(b -> RequestBody.create(b.bytes(), mediaType(b)))
                .orElseGet(() -> request.method().name().equals("GET")
                        || request.method().name().equals("HEAD")
                        ? null
                        : RequestBody.create(new byte[0], MediaType.parse("application/octet-stream")));
        rb.method(request.method().name(), requestBody);

        Instant start = Instant.now();
        try (Response response = client.newCall(rb.build()).execute()) {
            Duration elapsed = Duration.between(start, Instant.now());
            byte[] bytes = response.body() != null ? response.body().bytes() : new byte[0];
            HttpHeaders.Builder hb = HttpHeaders.builder();
            response.headers().toMultimap().forEach((name, values) ->
                    values.forEach(v -> hb.add(name, v)));
            return new ImmutableHttpResponse(
                    response.code(),
                    Optional.ofNullable(response.message()),
                    hb.build(),
                    List.of(),
                    bytes,
                    elapsed,
                    Map.of("engine", "okhttp"));
        } catch (IOException e) {
            if (e instanceof java.net.SocketTimeoutException) {
                throw new SinaqTimeoutException("OkHttp timeout for " + request.uri(), e, correlationId);
            }
            throw new SinaqEngineException("OkHttp failure for " + request.uri() + ": " + e.getMessage(),
                    e, correlationId);
        }
    }

    private static MediaType mediaType(HttpBody body) {
        return body.contentType()
                .map(MediaType::parse)
                .orElse(MediaType.parse("application/octet-stream"));
    }

    @Override
    public String name() {
        return "okhttp";
    }
}
