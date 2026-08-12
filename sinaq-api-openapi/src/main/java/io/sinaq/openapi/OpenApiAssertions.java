package io.sinaq.openapi;

import io.sinaq.api.response.ApiResponse;

/** Fluent OpenAPI checks on {@link ApiResponse} (V4). */
public final class OpenApiAssertions {

    private OpenApiAssertions() {}

    public static ApiResponse expect(ApiResponse response, OpenApiValidator validator, String path) {
        validator.validateResponse(response.request().method().name(), path, response.status());
        return response;
    }
}
