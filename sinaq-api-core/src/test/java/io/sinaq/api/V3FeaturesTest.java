package io.sinaq.api;

import io.sinaq.api.client.ApiClient;
import io.sinaq.api.client.Sinaq;
import io.sinaq.api.contract.ContractExpectation;
import io.sinaq.api.contract.ContractVerifier;
import io.sinaq.api.har.HarExporter;
import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpResponse;
import io.sinaq.api.http.ImmutableHttpResponse;
import io.sinaq.api.parallel.ParallelBatch;
import io.sinaq.api.recording.ExchangeRecorder;
import io.sinaq.api.request.RequestSpec;
import io.sinaq.api.SinaqRuntime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class V3FeaturesTest {

    private final ExchangeRecorder recorder = new ExchangeRecorder();

    @AfterEach
    void tearDown() {
        SinaqRuntime.publisher().unregister(recorder);
        recorder.clear();
    }

  @Test
  void multipartGraphqlAndParallelBatch() {
        AtomicInteger calls = new AtomicInteger();
        HttpEngine stub = new HttpEngine() {
            @Override
            public HttpResponse execute(io.sinaq.api.http.HttpRequest request) {
                calls.incrementAndGet();
                String body = request.body().map(b -> b.asString()).orElse("");
                return new ImmutableHttpResponse(200, HttpHeaders.empty(),
                        ("{\"ok\":true,\"body\":\"" + body.replace("\"", "'") + "\"}")
                                .getBytes(), Duration.ZERO);
            }
            @Override
            public String name() { return "stub"; }
        };
        ApiClient api = Sinaq.emptyClient().baseUrl("http://localhost").engine(stub).build();
        api.post("/g").graphql("{ x }").execute().expectStatus(200);
        api.post("/u").multiPart("a", "1").execute().expectStatus(200);

        Map<String, java.util.function.Supplier<RequestSpec>> batch = Map.of(
                "a", () -> api.get("/1"),
                "b", () -> api.get("/2"));
        ParallelBatch.ParallelResult result = ParallelBatch.execute(batch, 2, Duration.ofSeconds(5));
        assertThat(result.all()).hasSize(2);
        assertThat(calls.get()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void recordingHarAndContract() {
        SinaqRuntime.publisher().register(recorder);
        HttpEngine stub = new HttpEngine() {
            @Override
            public HttpResponse execute(io.sinaq.api.http.HttpRequest request) {
                return new ImmutableHttpResponse(200, HttpHeaders.empty(),
                        "{\"loan\":true}".getBytes(), Duration.ZERO);
            }
            @Override
            public String name() { return "stub"; }
        };
        ApiClient api = Sinaq.emptyClient().baseUrl("http://localhost").engine(stub).build();
        api.get("/loan").execute();
        assertThat(recorder.exchanges()).hasSize(1);
        String har = HarExporter.toHarJson(recorder.exchanges());
        assertThat(har).contains("/loan");
        ContractVerifier.verify(recorder.exchanges().get(0),
                ContractExpectation.builder().method("GET").responseBodyContains("loan").build());
    }
}
