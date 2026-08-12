package io.sinaq.api.client;

import io.sinaq.api.assertion.SchemaValidator;
import io.sinaq.api.config.SinaqConfig;
import io.sinaq.api.exception.SinaqConfigurationException;
import io.sinaq.api.extraction.ExtractionProvider;
import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpMethod;
import io.sinaq.api.masking.Masker;
import io.sinaq.api.plugin.PluginRegistry;
import io.sinaq.api.plugin.SinaqPlugin;
import io.sinaq.api.request.RequestSpec;
import io.sinaq.api.serialization.SerializationProvider;
import io.sinaq.api.template.RequestTemplate;
import io.sinaq.api.template.RequestTemplateRegistry;
import io.sinaq.api.cache.ResponseCache;
import io.sinaq.api.interceptor.RequestInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable, thread-safe API client — the entry point of the DSL (spec §10).
 * One instance is shared by all parallel tests; every call to {@code get/post/...}
 * creates a fresh thread-confined {@link RequestSpec}.
 */
public final class ApiClient implements AutoCloseable {

    private final SinaqConfig config;
    private final HttpEngine engine;
    private final SerializationProvider serializer;
    private final ExtractionProvider extractor;
    private final SchemaValidator schemaValidator;
    private final Masker masker;
    private final RequestTemplateRegistry templates;
    private final List<RequestInterceptor> interceptors;
    private final ResponseCache responseCache;

    private ApiClient(Builder b) {
        this.config = b.config;
        this.engine = b.engine;
        this.serializer = b.serializer;
        this.extractor = b.extractor;
        this.schemaValidator = b.schemaValidator;
        this.masker = b.masker;
        this.templates = b.templates;
        this.interceptors = List.copyOf(b.interceptors);
        this.responseCache = b.responseCache;
    }

    public RequestSpec get(String path)     { return request(HttpMethod.GET, path); }
    public RequestSpec post(String path)    { return request(HttpMethod.POST, path); }
    public RequestSpec put(String path)     { return request(HttpMethod.PUT, path); }
    public RequestSpec patch(String path)   { return request(HttpMethod.PATCH, path); }
    public RequestSpec delete(String path)  { return request(HttpMethod.DELETE, path); }
    public RequestSpec head(String path)    { return request(HttpMethod.HEAD, path); }
    public RequestSpec options(String path) { return request(HttpMethod.OPTIONS, path); }

    public RequestSpec request(HttpMethod method, String path) {
        return new RequestSpec(this, method, path);
    }

    /** Starts a request from a registered template (V2). */
    public RequestSpec fromTemplate(String name) {
        RequestTemplate t = templates.get(name);
        return t.apply(request(t.method(), t.path()));
    }

    public SinaqConfig config()                    { return config; }
    public HttpEngine engine()                     { return engine; }
    public SerializationProvider serializer()      { return serializer; }
    public ExtractionProvider extractor()          { return extractor; }
    public SchemaValidator schemaValidator()       { return schemaValidator; }
    public Masker masker()                         { return masker; }
    public RequestTemplateRegistry templates()     { return templates; }
    public List<RequestInterceptor> interceptors() { return interceptors; }
    public ResponseCache responseCache()           { return responseCache; }

    @Override
    public void close() {
        try {
            engine.close();
        } catch (Exception e) {
            throw new SinaqConfigurationException("Failed to close HTTP engine", e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Mutable builder. NOT thread-safe; confine to one thread. */
    public static final class Builder {

        private SinaqConfig config = SinaqConfig.builder().build();
        private HttpEngine engine;
        private SerializationProvider serializer = SerializationProvider.builtIn();
        private ExtractionProvider extractor = ExtractionProvider.builtIn();
        private SchemaValidator schemaValidator = SchemaValidator.unsupported();
        private Masker masker = Masker.defaults();
        private RequestTemplateRegistry templates = new RequestTemplateRegistry();
        private final List<RequestInterceptor> interceptors = new ArrayList<>();
        private ResponseCache responseCache = new ResponseCache();
        private boolean pluginsApplied;

        Builder() {}

        public Builder config(SinaqConfig config) {
            this.config = Objects.requireNonNull(config, "config");
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.config = config.toBuilder().baseUrl(baseUrl).build();
            return this;
        }

        public Builder profile(io.sinaq.api.config.EnvironmentProfile profile) {
            this.config = config.toBuilder()
                    .environment(profile.id())
                    .build();
            return this;
        }

        public Builder defaultHeader(String name, String value) {
            this.config = config.toBuilder().defaultHeader(name, value).build();
            return this;
        }

        public Builder timeout(io.sinaq.api.http.HttpTimeout timeout) {
            this.config = config.toBuilder().timeout(timeout).build();
            return this;
        }

        public Builder defaultAuth(io.sinaq.api.auth.AuthProvider auth) {
            this.config = config.toBuilder().defaultAuth(auth).build();
            return this;
        }

        public Builder defaultRetry(io.sinaq.api.config.RetryPolicy policy) {
            this.config = config.toBuilder().defaultRetry(policy).build();
            return this;
        }

        public Builder engine(HttpEngine engine) {
            this.engine = Objects.requireNonNull(engine, "engine");
            return this;
        }

        public Builder serializationProvider(SerializationProvider provider) {
            this.serializer = Objects.requireNonNull(provider, "provider");
            return this;
        }

        public Builder extractionProvider(ExtractionProvider provider) {
            this.extractor = Objects.requireNonNull(provider, "provider");
            return this;
        }

        public Builder schemaValidator(SchemaValidator validator) {
            this.schemaValidator = Objects.requireNonNull(validator, "validator");
            return this;
        }

        public Builder masker(Masker masker) {
            this.masker = Objects.requireNonNull(masker, "masker");
            return this;
        }

        public Builder templates(RequestTemplateRegistry registry) {
            this.templates = Objects.requireNonNull(registry, "registry");
            return this;
        }

        public Builder registerTemplate(RequestTemplate template) {
            this.templates.register(template);
            return this;
        }

        public Builder interceptor(RequestInterceptor interceptor) {
            this.interceptors.add(Objects.requireNonNull(interceptor, "interceptor"));
            return this;
        }

        public Builder interceptors(List<RequestInterceptor> interceptors) {
            this.interceptors.clear();
            if (interceptors != null) {
                this.interceptors.addAll(interceptors);
            }
            return this;
        }

        public Builder responseCache(ResponseCache cache) {
            this.responseCache = cache != null ? cache : new ResponseCache();
            return this;
        }

        public Builder plugin(SinaqPlugin plugin) {
            PluginRegistry.global().register(plugin);
            plugin.onClientBuild(this);
            return this;
        }

        public ApiClient build() {
            if (!pluginsApplied) {
                PluginRegistry.global().plugins().forEach(p -> p.onClientBuild(this));
                pluginsApplied = true;
            }
            if (engine == null) {
                throw new SinaqConfigurationException(
                        "No HttpEngine configured. Add sinaq-api-jdk or sinaq-api-restassured "
                        + "and call .engine(new JdkHttpEngine()) / .engine(new RestAssuredEngine()).");
            }
            return new ApiClient(this);
        }
    }
}
