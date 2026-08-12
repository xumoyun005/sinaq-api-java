package io.sinaq.api.auth;

import io.sinaq.api.request.RequestSpec;

import java.util.Objects;

/** API key in a configurable header (default {@code X-Api-Key}). Immutable. */
public final class ApiKeyAuth implements AuthProvider {

    public static final String DEFAULT_HEADER = "X-Api-Key";

    private final String headerName;
    private final String key;

    public ApiKeyAuth(String key) {
        this(DEFAULT_HEADER, key);
    }

    public ApiKeyAuth(String headerName, String key) {
        this.headerName = Objects.requireNonNull(headerName, "headerName");
        this.key = Objects.requireNonNull(key, "key");
    }

    @Override
    public void apply(RequestSpec spec) {
        spec.header(headerName, key);
    }
}
