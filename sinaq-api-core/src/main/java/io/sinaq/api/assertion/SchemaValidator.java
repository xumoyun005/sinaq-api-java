package io.sinaq.api.assertion;

import io.sinaq.api.exception.SinaqConfigurationException;

/**
 * JSON Schema validation SPI (V2). Default rejects — add {@code sinaq-api-jsonschema}.
 */
public interface SchemaValidator {

    void validate(String jsonBody, String schemaJson);

    static SchemaValidator unsupported() {
        return (body, schema) -> {
            throw new SinaqConfigurationException(
                    "JSON Schema validation requires the sinaq-api-jsonschema module "
                    + "and ApiClient.builder().schemaValidator(...)");
        };
    }
}
