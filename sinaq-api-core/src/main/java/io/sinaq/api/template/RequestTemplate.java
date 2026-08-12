package io.sinaq.api.template;

import io.sinaq.api.http.HttpMethod;
import io.sinaq.api.request.RequestSpec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable named request blueprint (V2). Apply to a fresh {@link RequestSpec}.
 */
public final class RequestTemplate {

    private final String name;
    private final HttpMethod method;
    private final String path;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final Object body;

    private RequestTemplate(Builder b) {
        this.name = b.name;
        this.method = b.method;
        this.path = b.path;
        this.headers = Map.copyOf(b.headers);
        this.queryParams = Map.copyOf(b.queryParams);
        this.body = b.body;
    }

    public String name()              { return name; }
    public HttpMethod method()        { return method; }
    public String path()              { return path; }
    public Map<String, String> headers()     { return headers; }
    public Map<String, String> queryParams() { return queryParams; }
    public Object body()              { return body; }

    /** Applies this template onto {@code spec} (does not change method/path). */
    public RequestSpec apply(RequestSpec spec) {
        headers.forEach(spec::header);
        queryParams.forEach(spec::queryParam);
        if (body != null) {
            spec.body(body);
        }
        return spec;
    }

    public static Builder builder(String name, HttpMethod method, String path) {
        return new Builder(name, method, path);
    }

    public static final class Builder {
        private final String name;
        private final HttpMethod method;
        private final String path;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private final Map<String, String> queryParams = new LinkedHashMap<>();
        private Object body;

        private Builder(String name, HttpMethod method, String path) {
            this.name = Objects.requireNonNull(name);
            this.method = Objects.requireNonNull(method);
            this.path = Objects.requireNonNull(path);
        }

        public Builder header(String n, String v)   { headers.put(n, v); return this; }
        public Builder queryParam(String n, String v) { queryParams.put(n, v); return this; }
        public Builder body(Object body)            { this.body = body; return this; }

        public RequestTemplate build() {
            return new RequestTemplate(this);
        }
    }
}
