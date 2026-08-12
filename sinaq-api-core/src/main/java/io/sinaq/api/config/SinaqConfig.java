package io.sinaq.api.config;

import io.sinaq.api.auth.AuthProvider;
import io.sinaq.api.exception.SinaqConfigurationException;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpTimeout;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable configuration snapshot (spec §16).
 *
 * <p>Layering: Global (files/env/system props) → Environment → Client → Request.
 * A snapshot is captured when it is built — the framework never re-reads external
 * sources mid-run, so parallel behaviour is deterministic. Thread-safe.</p>
 */
public final class SinaqConfig {

    public static final String DEFAULT_ENVIRONMENT = "default";

    private final String baseUrl;             // nullable until a client requires it
    private final String environment;
    private final HttpTimeout timeout;
    private final HttpHeaders defaultHeaders;
    private final AuthProvider defaultAuth;   // nullable
    private final RetryPolicy defaultRetry;

    private SinaqConfig(Builder b) {
        this.baseUrl = b.baseUrl;
        this.environment = b.environment;
        this.timeout = b.timeout;
        this.defaultHeaders = b.defaultHeaders;
        this.defaultAuth = b.defaultAuth;
        this.defaultRetry = b.defaultRetry;
    }

    public Optional<String> baseUrl()   { return Optional.ofNullable(baseUrl); }
    public String environment()         { return environment; }
    public HttpTimeout timeout()        { return timeout; }
    public HttpHeaders defaultHeaders() { return defaultHeaders; }
    /** Auth applied to every request unless the request sets its own. */
    public Optional<AuthProvider> defaultAuth() { return Optional.ofNullable(defaultAuth); }
    /** Transport retry defaults for requests that do not override retry. */
    public RetryPolicy defaultRetry()   { return defaultRetry; }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder pre-filled with this config — for layered overrides. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.baseUrl = baseUrl;
        b.environment = environment;
        b.timeout = timeout;
        b.defaultHeaders = defaultHeaders;
        b.defaultAuth = defaultAuth;
        b.defaultRetry = defaultRetry;
        return b;
    }

    @Override
    public String toString() {
        return "SinaqConfig[env=" + environment + ", baseUrl=" + baseUrl
                + ", timeout=" + timeout + ", defaultHeaders=" + defaultHeaders.size() + "]";
    }

    /** Mutable builder. NOT thread-safe; confine to one thread. */
    public static final class Builder {

        private String baseUrl;
        private String environment = DEFAULT_ENVIRONMENT;
        private HttpTimeout timeout = HttpTimeout.defaults();
        private HttpHeaders defaultHeaders = HttpHeaders.empty();
        private AuthProvider defaultAuth;
        private RetryPolicy defaultRetry = RetryPolicy.none();

        private Builder() {}

        /** Sets and validates the base URL (http/https, absolute). */
        public Builder baseUrl(String url) {
            if (url != null) {
                URI uri;
                try {
                    uri = URI.create(url);
                } catch (IllegalArgumentException e) {
                    throw new SinaqConfigurationException("Invalid " + ConfigKeys.BASE_URL + ": " + url, e);
                }
                if (!uri.isAbsolute() || uri.getScheme() == null
                        || !(uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
                    throw new SinaqConfigurationException(
                            "Invalid " + ConfigKeys.BASE_URL + " (must be absolute http/https): " + url);
                }
            }
            this.baseUrl = url;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = Objects.requireNonNull(environment, "environment");
            return this;
        }

        public Builder timeout(HttpTimeout timeout) {
            this.timeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        public Builder connectTimeout(Duration connect) {
            this.timeout = HttpTimeout.of(connect, timeout.read());
            return this;
        }

        public Builder readTimeout(Duration read) {
            this.timeout = HttpTimeout.of(timeout.connect(), read);
            return this;
        }

        public Builder defaultHeaders(HttpHeaders headers) {
            this.defaultHeaders = Objects.requireNonNull(headers, "defaultHeaders");
            return this;
        }

        public Builder defaultHeader(String name, String value) {
            this.defaultHeaders = defaultHeaders.toBuilder().set(name, value).build();
            return this;
        }

        public Builder defaultAuth(AuthProvider auth) {
            this.defaultAuth = auth;
            return this;
        }

        public Builder defaultRetry(RetryPolicy policy) {
            this.defaultRetry = Objects.requireNonNull(policy, "policy");
            return this;
        }

        public SinaqConfig build() {
            return new SinaqConfig(this);
        }
    }
}
