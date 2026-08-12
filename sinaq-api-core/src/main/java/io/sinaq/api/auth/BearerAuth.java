package io.sinaq.api.auth;

import io.sinaq.api.request.RequestSpec;

import java.util.Objects;

/** {@code Authorization: Bearer <token>}. Immutable. */
public final class BearerAuth implements AuthProvider {

    private final String token;

    public BearerAuth(String token) {
        this.token = Objects.requireNonNull(token, "token");
    }

    @Override
    public void apply(RequestSpec spec) {
        spec.header("Authorization", "Bearer " + token);
    }
}
