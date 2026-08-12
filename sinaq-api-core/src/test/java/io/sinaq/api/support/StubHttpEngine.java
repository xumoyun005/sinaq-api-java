package io.sinaq.api.support;

import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpRequest;
import io.sinaq.api.http.HttpResponse;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** Thread-safe stub {@link HttpEngine} for unit tests. */
public final class StubHttpEngine implements HttpEngine {

    private final AtomicReference<Function<HttpRequest, HttpResponse>> handler =
            new AtomicReference<>(req -> StubHttpResponse.json(200, "{}"));
    private final AtomicReference<HttpRequest> lastRequest = new AtomicReference<>();

    public void respond(Function<HttpRequest, HttpResponse> handler) {
        this.handler.set(handler);
    }

    public void respondJson(int status, String json) {
        respond(req -> StubHttpResponse.json(status, json));
    }

    public HttpRequest lastRequest() {
        return lastRequest.get();
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
        lastRequest.set(request);
        return handler.get().apply(request);
    }

    @Override
    public String name() {
        return "stub";
    }
}
