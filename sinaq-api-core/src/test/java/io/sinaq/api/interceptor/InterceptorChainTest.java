package io.sinaq.api.interceptor;

import io.sinaq.api.client.Sinaq;
import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpResponse;
import io.sinaq.api.http.ImmutableHttpResponse;
import io.sinaq.api.request.ApiRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class InterceptorChainTest {

    @Test
    void afterInterceptorsRunInReverseOrder() {
        List<String> order = new ArrayList<>();
        RequestInterceptor first = new RequestInterceptor() {
            @Override
            public HttpResponse afterResponse(ApiRequest request, HttpResponse response) {
                order.add("after-1");
                return response;
            }
        };
        RequestInterceptor second = new RequestInterceptor() {
            @Override
            public HttpResponse afterResponse(ApiRequest request, HttpResponse response) {
                order.add("after-2");
                return new ImmutableHttpResponse(202, response.headers(), response.rawBody(),
                        response.responseTime());
            }
        };
        HttpEngine stub = new HttpEngine() {
            @Override
            public HttpResponse execute(io.sinaq.api.http.HttpRequest request) {
                return new ImmutableHttpResponse(200, HttpHeaders.empty(), new byte[0], Duration.ZERO);
            }
            @Override
            public String name() { return "stub"; }
        };
        var api = Sinaq.emptyClient().baseUrl("http://localhost").engine(stub)
                .interceptor(first).interceptor(second).build();
        assertThat(api.get("/x").execute().status()).isEqualTo(202);
        assertThat(order).containsExactly("after-2", "after-1");
    }
}
