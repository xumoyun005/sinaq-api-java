package io.sinaq.api.auth;

import io.sinaq.api.request.RequestSpec;

import java.util.Objects;

/** Applies a {@link TokenProvider} as Bearer auth (V2). */
public final class BearerTokenAuth implements AuthProvider {

    private final TokenProvider tokens;

    public BearerTokenAuth(TokenProvider tokens) {
        this.tokens = Objects.requireNonNull(tokens, "tokens");
    }

    @Override
    public void apply(RequestSpec spec) {
        new BearerAuth(tokens.accessToken()).apply(spec);
    }
}
