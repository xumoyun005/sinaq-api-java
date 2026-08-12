package io.sinaq.api.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, ordered, multi-value query parameter collection.
 * Values are stored unencoded; URL encoding happens at request build time.
 * Thread-safe.
 */
public final class HttpQueryParams {

    /** One name/value pair, order-preserving. */
    public record Param(String name, String value) {
        public Param {
            Objects.requireNonNull(name, "param name");
            Objects.requireNonNull(value, "param value");
        }
    }

    private static final HttpQueryParams EMPTY = new HttpQueryParams(List.of());

    private final List<Param> params;

    private HttpQueryParams(List<Param> params) {
        this.params = params;
    }

    public static HttpQueryParams empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** All pairs in insertion order. Unmodifiable. */
    public List<Param> asList() {
        return params;
    }

    public Optional<String> first(String name) {
        return params.stream().filter(p -> p.name().equals(name)).map(Param::value).findFirst();
    }

    public List<String> all(String name) {
        return params.stream().filter(p -> p.name().equals(name)).map(Param::value).toList();
    }

    public int size() {
        return params.size();
    }

    public boolean isEmpty() {
        return params.isEmpty();
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        params.forEach(p -> b.add(p.name(), p.value()));
        return b;
    }

    @Override
    public String toString() {
        return "HttpQueryParams" + params;
    }

    /** Mutable builder. NOT thread-safe; confine to one thread. */
    public static final class Builder {

        private final List<Param> params = new ArrayList<>();

        private Builder() {}

        /** Adds a pair; value is converted with {@code String.valueOf}. */
        public Builder add(String name, Object value) {
            params.add(new Param(name, String.valueOf(value)));
            return this;
        }

        public HttpQueryParams build() {
            return params.isEmpty() ? EMPTY : new HttpQueryParams(List.copyOf(params));
        }
    }
}
