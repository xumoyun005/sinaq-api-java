package io.sinaq.api.cache;

import org.junit.jupiter.api.Test;

import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.ImmutableHttpResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class ResponseCacheTest {

    @Test
    void storesAndReturnsCopy() {
        ResponseCache cache = new ResponseCache();
        ImmutableHttpResponse response = new ImmutableHttpResponse(
                200, HttpHeaders.empty(), "{\"ok\":true}".getBytes(), Duration.ZERO);
        cache.put("k1", response);
        assertThat(cache.get("k1").statusCode()).isEqualTo(200);
        assertThat(cache.size()).isEqualTo(1);
        cache.invalidate("k1");
        assertThat(cache.get("k1")).isNull();
        assertThat(cache.get(null)).isNull();
        cache.put("k2", response);
        cache.clear();
        assertThat(cache.size()).isZero();
    }
}
