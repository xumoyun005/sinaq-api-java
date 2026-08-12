package io.sinaq.openapi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OpenApiValidatorTest {

    private static final String SPEC = """
            {
              "openapi": "3.0.0",
              "paths": {
                "/health": {
                  "get": { "responses": { "200": { "description": "ok" } } }
                }
              }
            }
            """;

    @Test
    void validatesAllowedPathAndStatus() {
        new OpenApiValidator(SPEC).validateResponse("GET", "/health", 200);
    }

    @Test
    void rejectsUnknownPath() {
        assertThatThrownBy(() -> new OpenApiValidator(SPEC).validateResponse("GET", "/missing", 200))
                .isInstanceOf(io.sinaq.api.exception.SinaqAssertionException.class);
    }
}
