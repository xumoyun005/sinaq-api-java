package io.sinaq.api.auth;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Wraps a token supplier with cache + refresh on expiry (V2).
 */
public final class RefreshingTokenProvider implements TokenProvider {

    private final TokenCache cache;
    private final Supplier<TokenResponse> fetcher;

    public RefreshingTokenProvider(Supplier<TokenResponse> fetcher) {
        this(new TokenCache(), fetcher);
    }

    public RefreshingTokenProvider(TokenCache cache, Supplier<TokenResponse> fetcher) {
        this.cache = Objects.requireNonNull(cache, "cache");
        this.fetcher = Objects.requireNonNull(fetcher, "fetcher");
    }

    @Override
    public String accessToken() {
        return cache.get().orElseGet(() -> {
            TokenResponse response = fetcher.get();
            cache.put(response.accessToken(), response.expiresAt());
            return response.accessToken();
        });
    }

    /** Forces the next call to re-fetch. */
    public void invalidate() {
        cache.invalidate();
    }

    /** Token endpoint response shape. */
    public record TokenResponse(String accessToken, java.time.Instant expiresAt) {
        public TokenResponse {
            Objects.requireNonNull(accessToken, "accessToken");
        }
    }
}
