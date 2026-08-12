package io.sinaq.api.request;

import io.sinaq.api.client.Sinaq;
import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.ImmutableHttpResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class RequestSpecV4Test {

    @Test
    void noCacheBypassesStore() {
        AtomicInteger calls = new AtomicInteger();
        HttpEngine stub = new HttpEngine() {
            @Override
            public io.sinaq.api.http.HttpResponse execute(io.sinaq.api.http.HttpRequest request) {
                calls.incrementAndGet();
                return new ImmutableHttpResponse(200, HttpHeaders.empty(), new byte[0], Duration.ZERO);
            }
            @Override
            public String name() { return "stub"; }
        };
        var api = Sinaq.emptyClient().baseUrl("http://localhost").engine(stub).build();
        api.get("/x").cacheKey("k").execute();
        api.get("/x").cacheKey("k").noCache().execute();
        assertThat(calls.get()).isEqualTo(2);
    }
}
