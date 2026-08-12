package io.sinaq.api;

import io.sinaq.api.client.Sinaq;
import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.ImmutableHttpResponse;
import io.sinaq.api.recording.RecordedExchange;
import io.sinaq.api.replay.ReplayPlayer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class V4FeaturesTest {

    @Test
    void responseCacheAvoidsSecondEngineCall() {
        AtomicInteger calls = new AtomicInteger();
        HttpEngine stub = new HttpEngine() {
            @Override
            public io.sinaq.api.http.HttpResponse execute(io.sinaq.api.http.HttpRequest request) {
                calls.incrementAndGet();
                return new ImmutableHttpResponse(200, HttpHeaders.empty(), "{\"v\":1}".getBytes(), Duration.ZERO);
            }
            @Override
            public String name() { return "stub"; }
        };
        var api = Sinaq.emptyClient().baseUrl("http://localhost").engine(stub).build();
        api.get("/cached").cacheKey("cards").execute();
        api.get("/cached").cacheKey("cards").execute();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void replayPlayerReplaysRecordedExchange() {
        AtomicInteger calls = new AtomicInteger();
        HttpEngine stub = new HttpEngine() {
            @Override
            public io.sinaq.api.http.HttpResponse execute(io.sinaq.api.http.HttpRequest request) {
                calls.incrementAndGet();
                return new ImmutableHttpResponse(200, HttpHeaders.empty(), "{}".getBytes(), Duration.ZERO);
            }
            @Override
            public String name() { return "stub"; }
        };
        var api = Sinaq.emptyClient().baseUrl("http://localhost").engine(stub).build();
        RecordedExchange ex = new RecordedExchange(
                "r1", "GET", "http://localhost/loan", Map.of(), "",
                200, Map.of(), "{}", 1, Instant.now());
        ReplayPlayer.ReplayResult result = ReplayPlayer.replay(api, List.of(ex));
        assertThat(result.replayedCount()).isEqualTo(1);
        assertThat(calls.get()).isEqualTo(1);
    }
}
