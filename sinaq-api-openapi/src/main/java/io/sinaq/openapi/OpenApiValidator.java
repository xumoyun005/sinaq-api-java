package io.sinaq.openapi;

import io.sinaq.api.client.ApiClient;
import io.sinaq.api.exception.SinaqAssertionException;
import io.sinaq.api.internal.json.Json;
import io.sinaq.api.plugin.SinaqPlugin;

import java.util.Map;
import java.util.Objects;

/**
 * Lightweight OpenAPI 3 path/status validator (V4).
 */
public final class OpenApiValidator {

    private final String specJson;

    public OpenApiValidator(String openApiSpecJson) {
        this.specJson = Objects.requireNonNull(openApiSpecJson, "openApiSpecJson");
    }

    public void validateResponse(String method, String path, int status) {
        Object root = Json.parse(specJson);
        if (!(root instanceof Map<?, ?> map)) {
            throw new io.sinaq.api.exception.SinaqConfigurationException("Invalid OpenAPI JSON");
        }
        Object paths = map.get("paths");
        if (!(paths instanceof Map<?, ?> pathMap)) {
            throw new SinaqAssertionException("OpenAPI spec has no paths");
        }
        Object pathItem = pathMap.get(path);
        if (pathItem == null) {
            throw new SinaqAssertionException("OpenAPI path not found: " + path);
        }
        if (!(pathItem instanceof Map<?, ?> ops)) {
            throw new SinaqAssertionException("Invalid OpenAPI path item: " + path);
        }
        Object operation = ops.get(method.toLowerCase());
        if (operation == null) {
            throw new SinaqAssertionException("OpenAPI method not allowed: " + method + " " + path);
        }
        if (!(operation instanceof Map<?, ?> opMap)) {
            throw new SinaqAssertionException("Invalid OpenAPI operation: " + method + " " + path);
        }
        Object responses = opMap.get("responses");
        if (!(responses instanceof Map<?, ?> responseMap)) {
            throw new SinaqAssertionException("OpenAPI operation has no responses: " + method + " " + path);
        }
        String statusKey = String.valueOf(status);
        if (!responseMap.containsKey(statusKey) && !responseMap.containsKey("default")) {
            throw new SinaqAssertionException(
                    "OpenAPI response status not defined: " + status + " for " + method + " " + path);
        }
    }

    /** Auto-registers OpenAPI validation helper when used as a plugin. */
    public static final class Plugin implements SinaqPlugin {
        private final OpenApiValidator validator;

        public Plugin(String openApiSpecJson) {
            this.validator = new OpenApiValidator(openApiSpecJson);
        }

        @Override public String id() { return "openapi"; }
        @Override public void onClientBuild(ApiClient.Builder builder) {
            builder.registerTemplate(io.sinaq.api.template.RequestTemplate.builder(
                    "openapi-health", io.sinaq.api.http.HttpMethod.GET, "/health").build());
        }

        public OpenApiValidator validator() {
            return validator;
        }
    }
}
