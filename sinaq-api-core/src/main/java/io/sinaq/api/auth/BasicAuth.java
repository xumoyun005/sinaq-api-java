package io.sinaq.api.auth;

import io.sinaq.api.request.RequestSpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/** {@code Authorization: Basic base64(user:password)}. Immutable. */
public final class BasicAuth implements AuthProvider {

    private final String username;
    private final String password;

    public BasicAuth(String username, String password) {
        this.username = Objects.requireNonNull(username, "username");
        this.password = Objects.requireNonNull(password, "password");
    }

    @Override
    public void apply(RequestSpec spec) {
        String credentials = username + ":" + password;
        spec.header("Authorization", "Basic "
                + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
    }
}
