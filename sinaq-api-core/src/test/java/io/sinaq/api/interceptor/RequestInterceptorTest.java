package io.sinaq.api.interceptor;

import io.sinaq.api.client.ApiClient;
import io.sinaq.api.client.Sinaq;
import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpRequest;
import io.sinaq.api.http.HttpResponse;
import io.sinaq.api.http.ImmutableHttpResponse;
import io.sinaq.api.request.ApiRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class RequestInterceptorTest {

    @Test
    void interceptorsModifyRequestAndResponse() {
        AtomicReference<String> headerValue = new AtomicReference<>();
        HttpEngine stub = new HttpEngine() {
            @Override
            public HttpResponse execute(HttpRequest request) {
                headerValue.set(request.headers().first("X-Injected").orElse(null));
                return new ImmutableHttpResponse(200, HttpHeaders.empty(), new byte[0], Duration.ZERO);
            }
            @Override
            public String name() { return "stub"; }
        };
        ApiClient api = Sinaq.emptyClient()
                .baseUrl("http://localhost")
                .engine(stub)
                .interceptor(new RequestInterceptor() {
                    @Override
                    public ApiRequest beforeRequest(ApiRequest request) {
                        return new ApiRequest(
                                request.method(), request.uri(),
                                request.headers().toBuilder().set("X-Injected", "yes").build(),
                                request.cookies(), request.bodyOrNull(), request.timeout(), request.context());
                    }
                    @Override
                    public HttpResponse afterResponse(ApiRequest request, HttpResponse response) {
                        return new ImmutableHttpResponse(
                                201, response.headers(), response.rawBody(), response.responseTime());
                    }
                })
                .build();
        assertThat(api.get("/x").execute().status()).isEqualTo(201);
        assertThat(headerValue.get()).isEqualTo("yes");
    }
}
