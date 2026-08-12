package io.sinaq.api.auth;

import io.sinaq.api.request.RequestSpec;

import java.util.Objects;

/** Pre-set JWT bearer token (V2). For dynamic tokens use {@link BearerTokenAuth}. */
public final class JwtAuth implements AuthProvider {

    private final String jwt;

    public JwtAuth(String jwt) {
        this.jwt = Objects.requireNonNull(jwt, "jwt");
    }

    @Override
    public void apply(RequestSpec spec) {
        spec.header("Authorization", "Bearer " + jwt);
    }
}
