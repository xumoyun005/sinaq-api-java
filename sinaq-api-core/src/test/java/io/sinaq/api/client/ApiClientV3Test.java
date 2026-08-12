package io.sinaq.api.client;

import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpResponse;
import io.sinaq.api.http.ImmutableHttpResponse;
import io.sinaq.api.interceptor.RequestInterceptor;
import io.sinaq.api.request.ApiRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ApiClientV3Test {

    @Test
    void registersMultipleInterceptors() {
        List<String> order = new ArrayList<>();
        RequestInterceptor first = new RequestInterceptor() {
            @Override
            public ApiRequest beforeRequest(ApiRequest request) {
                order.add("before-1");
                return request;
            }
        };
        RequestInterceptor second = new RequestInterceptor() {
            @Override
            public ApiRequest beforeRequest(ApiRequest request) {
                order.add("before-2");
                return request;
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
        ApiClient api = Sinaq.emptyClient()
                .baseUrl("http://localhost")
                .engine(stub)
                .interceptor(first)
                .interceptor(second)
                .build();
        api.get("/x").execute();
        assertThat(order).containsExactly("before-1", "before-2");
        assertThat(api.interceptors()).hasSize(2);
    }
}
