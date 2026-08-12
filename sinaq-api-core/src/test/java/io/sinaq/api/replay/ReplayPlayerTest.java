package io.sinaq.api.replay;

import io.sinaq.api.client.Sinaq;
import io.sinaq.api.exception.SinaqAssertionException;
import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.ImmutableHttpResponse;
import io.sinaq.api.recording.RecordedExchange;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ReplayPlayerTest {

    @Test
    void failsOnStatusMismatch() {
        HttpEngine stub = new HttpEngine() {
            @Override
            public io.sinaq.api.http.HttpResponse execute(io.sinaq.api.http.HttpRequest request) {
                return new ImmutableHttpResponse(404, HttpHeaders.empty(), new byte[0], Duration.ZERO);
            }
            @Override
            public String name() { return "stub"; }
        };
        var api = Sinaq.emptyClient().baseUrl("http://localhost").engine(stub).build();
        RecordedExchange ex = new RecordedExchange(
                "r1", "GET", "http://localhost/x", Map.of(), "", 200, Map.of(), "{}", 1, Instant.now());
        assertThatThrownBy(() -> ReplayPlayer.replay(api, List.of(ex)))
                .isInstanceOf(SinaqAssertionException.class);
    }

    @Test
    void replaysPostWithBody() {
        HttpEngine stub = new HttpEngine() {
            @Override
            public io.sinaq.api.http.HttpResponse execute(io.sinaq.api.http.HttpRequest request) {
                return new ImmutableHttpResponse(201, HttpHeaders.empty(), new byte[0], Duration.ZERO);
            }
            @Override
            public String name() { return "stub"; }
        };
        var api = Sinaq.emptyClient().baseUrl("http://localhost").engine(stub).build();
        RecordedExchange ex = new RecordedExchange(
                "r1", "POST", "http://localhost/items", Map.of(), "{\"id\":1}",
                201, Map.of(), "{}", 1, Instant.now());
        assertThat(ReplayPlayer.replay(api, List.of(ex)).replayedCount()).isEqualTo(1);
    }
}
