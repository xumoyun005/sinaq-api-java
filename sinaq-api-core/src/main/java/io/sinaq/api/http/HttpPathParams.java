package io.sinaq.api.http;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable path parameter map for templated paths like {@code /users/{id}}.
 * Thread-safe.
 */
public final class HttpPathParams {

    private static final HttpPathParams EMPTY = new HttpPathParams(Map.of());

    private final Map<String, String> params;

    private HttpPathParams(Map<String, String> params) {
        this.params = params;
    }

    public static HttpPathParams empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> get(String name) {
        return Optional.ofNullable(params.get(name));
    }

    /** Snapshot in insertion order. Unmodifiable. */
    public Map<String, String> asMap() {
        return params;
    }

    public int size() {
        return params.size();
    }

    public boolean isEmpty() {
        return params.isEmpty();
    }

    @Override
    public String toString() {
        return "HttpPathParams" + params;
    }

    /** Mutable builder. NOT thread-safe; confine to one thread. */
    public static final class Builder {

        private final Map<String, String> params = new LinkedHashMap<>();

        private Builder() {}

        /** Sets a parameter; value is converted with {@code String.valueOf}. */
        public Builder set(String name, Object value) {
            Objects.requireNonNull(name, "path param name");
            params.put(name, String.valueOf(value));
            return this;
        }

        public HttpPathParams build() {
            return params.isEmpty() ? EMPTY
                    : new HttpPathParams(java.util.Collections.unmodifiableMap(new LinkedHashMap<>(params)));
        }
    }
}
