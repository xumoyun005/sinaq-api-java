package io.sinaq.api.distributed;

import io.sinaq.api.client.Sinaq;
import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.ImmutableHttpResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DistributedRunnerTest {

    @Test
    void emptyWorkersReturnsEmptyResult() {
        DistributedRunner.DistributedResult result = DistributedRunner.execute(
                client -> client.get("/x"), List.of(), 2, Duration.ofSeconds(1));
        assertThat(result.workers()).isEmpty();
        assertThat(result.allStatus(200)).isTrue();
    }

    @Test
    void runsAcrossWorkers() {
        HttpEngine stub = new HttpEngine() {
            @Override
            public io.sinaq.api.http.HttpResponse execute(io.sinaq.api.http.HttpRequest request) {
                return new ImmutableHttpResponse(200, HttpHeaders.empty(), new byte[0], Duration.ZERO);
            }
            @Override
            public String name() { return "stub"; }
        };
        var w1 = Sinaq.emptyClient().baseUrl("http://localhost").engine(stub).build();
        var w2 = Sinaq.emptyClient().baseUrl("http://localhost").engine(stub).build();
        DistributedRunner.DistributedResult result = DistributedRunner.execute(
                client -> client.get("/health"), List.of(w1, w2), 2, Duration.ofSeconds(5));
        assertThat(result.workers()).hasSize(2);
        assertThat(result.allStatus(200)).isTrue();
    }
}
