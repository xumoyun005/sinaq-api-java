package io.sinaq.api.auth;

import io.sinaq.api.request.RequestSpec;

/**
 * Authentication SPI (spec §12). Providers decorate the request (headers,
 * query params); they never touch the transport engine.
 * Implementations must be immutable/thread-safe.
 */
@FunctionalInterface
public interface AuthProvider {

    void apply(RequestSpec spec);
}
