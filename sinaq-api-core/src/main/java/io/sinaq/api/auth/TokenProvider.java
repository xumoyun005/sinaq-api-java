package io.sinaq.api.auth;

import java.time.Instant;
import java.util.Optional;

/**
 * Supplies access tokens (V2). Implementations must be thread-safe.
 */
public interface TokenProvider {

    /** Returns a valid access token, fetching or refreshing when needed. */
    String accessToken();

    /** Token expiry if known. */
    default Optional<Instant> expiresAt() {
        return Optional.empty();
    }
}
