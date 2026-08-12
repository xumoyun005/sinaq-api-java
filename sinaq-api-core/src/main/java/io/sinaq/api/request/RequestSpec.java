package io.sinaq.api.request;

import io.sinaq.api.SinaqRuntime;
import io.sinaq.api.auth.ApiKeyAuth;
import io.sinaq.api.auth.AuthProvider;
import io.sinaq.api.auth.BasicAuth;
import io.sinaq.api.auth.BearerAuth;
import io.sinaq.api.client.ApiClient;
import io.sinaq.api.config.RetryPolicy;
import io.sinaq.api.config.SinaqConfig;
import io.sinaq.api.context.RequestContext;
import io.sinaq.api.context.SinaqTestContextHolder;
import io.sinaq.api.context.TestContext;
import io.sinaq.api.events.EventType;
import io.sinaq.api.events.ReportEvent;
import io.sinaq.api.exception.SinaqConfigurationException;
import io.sinaq.api.exception.SinaqEngineException;
import io.sinaq.api.exception.SinaqException;
import io.sinaq.api.exception.SinaqTimeoutException;
import io.sinaq.api.graphql.GraphQl;
import io.sinaq.api.http.MultipartBody;
import io.sinaq.api.http.HttpBody;
import io.sinaq.api.http.HttpCookie;
import io.sinaq.api.interceptor.RequestInterceptor;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpMethod;
import io.sinaq.api.http.HttpResponse;
import io.sinaq.api.http.HttpTimeout;
import io.sinaq.api.metrics.RequestMetrics;
import io.sinaq.api.polling.PollSpec;
import io.sinaq.api.response.ApiResponse;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Mutable, THREAD-CONFINED fluent request builder (spec §10–11).
 *
 * <p>Create → configure → execute in one thread; never share an instance and
 * never reuse it after execution. {@code expect*} methods are sugar: the first
 * one triggers {@link #execute()} exactly once and delegates to the resulting
 * {@link ApiResponse} (ADR-006).</p>
 */
public final class RequestSpec {

    private final ApiClient client;
    private final HttpMethod method;
    private final String path;

    private final HttpHeaders.Builder headers = HttpHeaders.builder();
    private final Set<String> overriddenHeaderNames = new HashSet<>();
    private final Map<String, String> pathParams = new LinkedHashMap<>();
    private final List<String[]> queryParams = new ArrayList<>();
    private final List<HttpCookie> cookies = new ArrayList<>();

    private HttpBody body;
    private MultipartBody.Builder multipart;
    private AuthProvider auth;
    private HttpTimeout timeout;
    private RetryPolicy retry;
    private String correlationId;
    private String cacheKey;
    private boolean cachingEnabled = true;

    private ApiResponse response;   // set once by execute()

    /** Created by {@link ApiClient}; not for direct construction. */
    public RequestSpec(ApiClient client, HttpMethod method, String path) {
        this.client = Objects.requireNonNull(client, "client");
        this.method = Objects.requireNonNull(method, "method");
        this.path = Objects.requireNonNull(path, "path");
        this.retry = client.config().defaultRetry();
    }

    private RequestSpec(ApiClient client, HttpMethod method, String path, boolean internal) {
        this(client, method, path);
    }

    // ---------- building (spec §11) ----------

    /** Sets a header; first call for a name overrides any client default. */
    public RequestSpec header(String name, String value) {
        if (overriddenHeaderNames.add(name.toLowerCase(java.util.Locale.ROOT))) {
            headers.set(name, value);
        } else {
            headers.add(name, value);
        }
        return this;
    }

    public RequestSpec headers(Map<String, String> map) {
        map.forEach(this::header);
        return this;
    }

    public RequestSpec queryParam(String name, Object value) {
        queryParams.add(new String[]{Objects.requireNonNull(name, "name"), String.valueOf(value)});
        return this;
    }

    public RequestSpec queryParams(Map<String, ?> map) {
        map.forEach(this::queryParam);
        return this;
    }

    public RequestSpec pathParam(String name, Object value) {
        pathParams.put(Objects.requireNonNull(name, "name"), String.valueOf(value));
        return this;
    }

    public RequestSpec pathParams(Map<String, ?> map) {
        map.forEach(this::pathParam);
        return this;
    }

    public RequestSpec cookie(String name, String value) {
        cookies.add(HttpCookie.of(name, value));
        return this;
    }

    /** Serializes the object via the client's SerializationProvider as JSON. */
    public RequestSpec body(Object value) {
        if (value instanceof String s) {
            return json(s);
        }
        this.body = HttpBody.ofJson(client.serializer().serialize(value));
        return this;
    }

    /** Raw JSON body. */
    public RequestSpec json(String rawJson) {
        this.body = HttpBody.ofJson(rawJson);
        return this;
    }

    /** Plain text body. */
    public RequestSpec text(String text) {
        this.body = HttpBody.ofText(text);
        return this;
    }

    /** application/x-www-form-urlencoded body from ordered pairs. */
    public RequestSpec form(Map<String, ?> fields) {
        StringBuilder sb = new StringBuilder();
        fields.forEach((k, v) -> {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(encode(k)).append('=').append(encode(String.valueOf(v)));
        });
        this.body = HttpBody.ofBytes(sb.toString().getBytes(StandardCharsets.UTF_8), HttpBody.FORM);
        return this;
    }

    /** Multipart form field (V3). */
    public RequestSpec multiPart(String name, String value) {
        if (multipart == null) {
            multipart = MultipartBody.builder();
        }
        multipart.part(name, value);
        return this;
    }

    /** Multipart file upload (V3). */
    public RequestSpec multiPartFile(String name, byte[] bytes, String filename) {
        if (multipart == null) {
            multipart = MultipartBody.builder();
        }
        multipart.file(name, bytes, filename);
        return this;
    }

    /** GraphQL query body (V3). */
    public RequestSpec graphql(String query) {
        return body(GraphQl.query(query));
    }

    /** GraphQL query with variables (V3). */
    public RequestSpec graphql(String query, Map<String, ?> variables) {
        return body(GraphQl.query(query, variables));
    }

    public RequestSpec bearer(String token)                    { this.auth = new BearerAuth(token); return this; }
    public RequestSpec basicAuth(String user, String password) { this.auth = new BasicAuth(user, password); return this; }
    public RequestSpec apiKey(String key)                      { this.auth = new ApiKeyAuth(key); return this; }
    public RequestSpec auth(AuthProvider provider)             { this.auth = provider; return this; }

    public RequestSpec timeout(HttpTimeout timeout) {
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        return this;
    }

    public RequestSpec retry(RetryPolicy policy) {
        this.retry = Objects.requireNonNull(policy, "policy");
        return this;
    }

    public RequestSpec correlationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    /** Enables response caching under the given key (V4). */
    public RequestSpec cacheKey(String cacheKey) {
        this.cacheKey = Objects.requireNonNull(cacheKey, "cacheKey");
        return this;
    }

    /** Disables cache lookup/store for this request (V4). */
    public RequestSpec noCache() {
        this.cachingEnabled = false;
        return this;
    }

    /** Returns a copy of this spec for polling / parallel use (V2). */
    public RequestSpec duplicate() {
        if (response != null) {
            throw new SinaqException("Cannot duplicate an already-executed RequestSpec");
        }
        RequestSpec copy = new RequestSpec(client, method, path, true);
        headers.build().asMap().forEach((name, values) ->
                values.forEach(v -> copy.header(name, v)));
        copy.pathParams.putAll(this.pathParams);
        copy.queryParams.addAll(this.queryParams);
        copy.cookies.addAll(this.cookies);
        copy.body = this.body;
        copy.multipart = this.multipart != null ? this.multipart.copy() : null;
        copy.auth = this.auth;
        copy.timeout = this.timeout;
        copy.retry = this.retry;
        copy.correlationId = this.correlationId;
        copy.cacheKey = this.cacheKey;
        copy.cachingEnabled = this.cachingEnabled;
        return copy;
    }

    /** Poll until condition is met (V2). */
    public PollSpec poll() {
        return new PollSpec(this);
    }

    // ---------- execution ----------

    /** Builds the immutable request, runs it through the engine, returns the response. */
    public ApiResponse execute() {
        if (response != null) {
            throw new SinaqException("RequestSpec already executed — create a new spec per request");
        }
        SinaqConfig config = client.config();
        AuthProvider effectiveAuth = auth != null ? auth : config.defaultAuth().orElse(null);
        if (effectiveAuth != null) {
            effectiveAuth.apply(this);
        }
        ApiRequest request = new ApiRequest(
                method,
                buildUri(config),
                mergeHeaders(config),
                List.copyOf(cookies),
                resolveBody(),
                timeout != null ? timeout : config.timeout(),
                RequestContext.withCorrelationId(correlationId));

        publish(EventType.REQUEST_CREATED, request, Map.of(
                "method", method.name(),
                "url", request.uri().toString(),
                "headers", client.masker().maskHeaders(request.headers()),
                "body", maskedBodyPreview()));

        HttpResponse http;
        if (cacheKey != null && cachingEnabled) {
            HttpResponse cached = client.responseCache().get(cacheKey);
            if (cached != null) {
                publish(EventType.CACHE_HIT, request, Map.of("cacheKey", cacheKey));
                http = cached;
            } else {
                publish(EventType.CACHE_MISS, request, Map.of("cacheKey", cacheKey));
                http = executeWithRetry(applyInterceptorsBefore(request));
                http = applyInterceptorsAfter(request, http);
                client.responseCache().put(cacheKey, http);
            }
        } else {
            http = executeWithRetry(applyInterceptorsBefore(request));
            http = applyInterceptorsAfter(request, http);
        }

        ApiResponse api = new ApiResponse(request, http, client.serializer(),
                client.extractor(), client.schemaValidator(), client.masker());
        publishMetrics(request, http);
        publish(EventType.RESPONSE_RECEIVED, request, Map.of(
                "status", http.statusCode(),
                "responseTimeMs", http.responseTime().toMillis(),
                "headers", client.masker().maskHeaders(http.headers()),
                "body", preview(client.masker().maskText(api.text()))));
        this.response = api;
        return api;
    }

    private HttpResponse executeWithRetry(ApiRequest request) {
        int attempt = 0;
        while (true) {
            attempt++;
            publish(EventType.REQUEST_STARTED, request, Map.of("attempt", attempt));
            try {
                HttpResponse http = client.engine().execute(request);
                publish(EventType.REQUEST_SENT, request, Map.of("attempt", attempt));
                if (retry.enabled() && attempt < retry.maxAttempts()
                        && retry.onStatusCodes().contains(http.statusCode())) {
                    sleepBackoff();
                    continue;
                }
                return http;
            } catch (SinaqTimeoutException e) {
                if (retry.enabled() && retry.onTimeout() && attempt < retry.maxAttempts()) {
                    sleepBackoff();
                    continue;
                }
                publishError(request, e);
                throw e;
            } catch (SinaqEngineException e) {
                if (retry.enabled() && retry.onTransportError() && attempt < retry.maxAttempts()) {
                    sleepBackoff();
                    continue;
                }
                publishError(request, e);
                throw e;
            }
        }
    }

    private void sleepBackoff() {
        Duration backoff = retry.backoff();
        if (!backoff.isZero()) {
            try {
                Thread.sleep(backoff.toMillis());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new SinaqException("Interrupted during retry backoff", ie);
            }
        }
    }

    // ---------- assertion sugar (ADR-006) ----------

    public ApiResponse expectStatus(int expected)              { return ensureExecuted().expectStatus(expected); }
    public ApiResponse expect(String path, Object expected)    { return ensureExecuted().expect(path, expected); }
    public ApiResponse expectNotNull(String path)              { return ensureExecuted().expectNotNull(path); }
    public ApiResponse expectNull(String path)                 { return ensureExecuted().expectNull(path); }
    public ApiResponse expectHeader(String name, String value) { return ensureExecuted().expectHeader(name, value); }
    public ApiResponse expectEquals(String path, Object expected) { return ensureExecuted().expectEquals(path, expected); }
    public ApiResponse expectMatches(String regex)                 { return ensureExecuted().expectMatches(regex); }
    public ApiResponse expectArraySize(String path, int size)      { return ensureExecuted().expectArraySize(path, size); }
    public ApiResponse expectSchema(String schemaJson)             { return ensureExecuted().expectSchema(schemaJson); }
    public ApiResponse expectContains(String substring)        { return ensureExecuted().expectContains(substring); }
    public ApiResponse expectResponseTimeLessThan(Duration max){ return ensureExecuted().expectResponseTimeLessThan(max); }

    private ApiResponse ensureExecuted() {
        return response != null ? response : execute();
    }

    // ---------- internals ----------

    private URI buildUri(SinaqConfig config) {
        String resolvedPath = path;
        for (Map.Entry<String, String> e : pathParams.entrySet()) {
            resolvedPath = resolvedPath.replace("{" + e.getKey() + "}", encodePath(e.getValue()));
        }
        if (resolvedPath.contains("{")) {
            throw new SinaqConfigurationException("Unresolved path parameter in: " + resolvedPath);
        }
        String base;
        if (resolvedPath.startsWith("http://") || resolvedPath.startsWith("https://")) {
            base = resolvedPath;
        } else {
            String root = config.baseUrl().orElseThrow(() -> new SinaqConfigurationException(
                    "No baseUrl configured (set " + io.sinaq.api.config.ConfigKeys.BASE_URL
                    + " or ApiClient.builder().baseUrl(...))"));
            base = trimTrailingSlash(root) + ensureLeadingSlash(resolvedPath);
        }
        if (!queryParams.isEmpty()) {
            StringBuilder q = new StringBuilder();
            for (String[] p : queryParams) {
                if (!q.isEmpty()) q.append('&');
                q.append(encode(p[0])).append('=').append(encode(p[1]));
            }
            base = base + (base.indexOf('?') >= 0 ? "&" : "?") + q;
        }
        return URI.create(base);
    }

    private HttpHeaders mergeHeaders(SinaqConfig config) {
        HttpHeaders.Builder merged = config.defaultHeaders().toBuilder();
        HttpHeaders specHeaders = headers.build();
        for (String name : specHeaders.names()) {
            List<String> values = specHeaders.all(name);
            merged.set(name, values.get(0));
            for (int i = 1; i < values.size(); i++) {
                merged.add(name, values.get(i));
            }
        }
        if (resolveBody() != null && resolveBody().contentType().isPresent() && !specHeaders.contains("Content-Type")
                && !config.defaultHeaders().contains("Content-Type")) {
            merged.set("Content-Type", resolveBody().contentType().get());
        }
        return merged.build();
    }

    private HttpBody resolveBody() {
        if (multipart != null) {
            return multipart.build();
        }
        return body;
    }

    private ApiRequest applyInterceptorsBefore(ApiRequest request) {
        ApiRequest current = request;
        for (RequestInterceptor interceptor : client.interceptors()) {
            current = interceptor.beforeRequest(current);
            publish(EventType.INTERCEPTOR_APPLIED, current, Map.of(
                    "phase", "before",
                    "interceptor", interceptor.getClass().getName()));
        }
        return current;
    }

    private HttpResponse applyInterceptorsAfter(ApiRequest request, HttpResponse response) {
        HttpResponse current = response;
        List<RequestInterceptor> chain = client.interceptors();
        for (int i = chain.size() - 1; i >= 0; i--) {
            RequestInterceptor interceptor = chain.get(i);
            current = interceptor.afterResponse(request, current);
            publish(EventType.INTERCEPTOR_APPLIED, request, Map.of(
                    "phase", "after",
                    "interceptor", interceptor.getClass().getName()));
        }
        return current;
    }

    private String maskedBodyPreview() {
        HttpBody effective = resolveBody();
        return effective == null ? "" : preview(client.masker().maskText(effective.asString()));
    }

    private void publish(EventType type, ApiRequest request, Map<String, Object> payload) {
        RequestContext ctx = request.context();
        ReportEvent.Builder b = ReportEvent.builder(type, SinaqRuntime.executionContext().executionId())
                .requestId(ctx.requestId())
                .correlationId(ctx.correlationId().orElse(null))
                .payload(payload);
        TestContext test = SinaqTestContextHolder.get();
        if (test != null) {
            b.testId(test.testId());
        }
        SinaqRuntime.publisher().publish(b.build());
    }

    private void publishError(ApiRequest request, SinaqException e) {
        publish(EventType.ERROR_OCCURRED, request, Map.of(
                "error", e.getClass().getSimpleName(),
                "message", Optional.ofNullable(e.getMessage()).orElse("")));
    }

    private void publishMetrics(ApiRequest request, HttpResponse http) {
        long sent = request.body().map(b -> (long) b.bytes().length).orElse(0L);
        RequestMetrics metrics = new RequestMetrics(
                request.context().requestId(),
                http.responseTime(),
                request.timeout().connect(),
                request.timeout().read(),
                1,
                sent,
                http.rawBody().length,
                client.engine().name());
        SinaqRuntime.publisher().publish(ReportEvent.builder(
                        EventType.PERFORMANCE_RECORDED,
                        SinaqRuntime.executionContext().executionId())
                .requestId(request.context().requestId())
                .correlationId(request.context().correlationId().orElse(null))
                .payload(metrics.asPayload())
                .build());
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodePath(String s) {
        return encode(s).replace("%2F", "/");
    }

    private static String trimTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String ensureLeadingSlash(String s) {
        return s.startsWith("/") ? s : "/" + s;
    }

    private static String preview(String s) {
        return s.length() <= 2048 ? s : s.substring(0, 2048) + "...(truncated)";
    }
}
