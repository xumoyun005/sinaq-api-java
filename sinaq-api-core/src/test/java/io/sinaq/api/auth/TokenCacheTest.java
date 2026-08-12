package io.sinaq.api.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class TokenCacheTest {

    @Test
    void expiresCachedTokens() throws Exception {
        TokenCache cache = new TokenCache();
        cache.put("old", Instant.now().minusSeconds(1));
        assertThat(cache.get()).isEmpty();
        cache.put("fresh", Instant.now().plusSeconds(60));
        assertThat(cache.get()).contains("fresh");
        cache.invalidate();
        String fetched = cache.getOrFetch(() -> "fetched", Instant.now().plusSeconds(60));
        assertThat(fetched).isEqualTo("fetched");
        assertThat(cache.get()).contains("fetched");
    }
}
