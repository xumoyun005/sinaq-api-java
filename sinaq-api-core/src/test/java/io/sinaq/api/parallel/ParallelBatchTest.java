package io.sinaq.api.parallel;

import io.sinaq.api.client.Sinaq;
import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpResponse;
import io.sinaq.api.http.ImmutableHttpResponse;
import io.sinaq.api.request.RequestSpec;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

class ParallelBatchTest {

    @Test
    void emptyBatchReturnsEmptyResult() {
        assertThat(ParallelBatch.execute(Map.of(), 2, Duration.ofSeconds(1)).all()).isEmpty();
    }

    @Test
    void executesNamedRequests() {
        HttpEngine stub = new HttpEngine() {
            @Override
            public HttpResponse execute(io.sinaq.api.http.HttpRequest request) {
                return new ImmutableHttpResponse(200, HttpHeaders.empty(),
                        request.uri().toString().getBytes(), Duration.ZERO);
            }
            @Override
            public String name() { return "stub"; }
        };
        var api = Sinaq.emptyClient().baseUrl("http://localhost").engine(stub).build();
        Map<String, Supplier<RequestSpec>> batch = Map.of(
                "first", () -> api.get("/a"),
                "second", () -> api.get("/b"));
        ParallelBatch.ParallelResult result = ParallelBatch.execute(batch, 2, Duration.ofSeconds(5));
        assertThat(result.get("first").status()).isEqualTo(200);
        assertThat(result.get("second").text()).contains("/b");
    }
}
