package io.sinaq.jsonschema;

import io.sinaq.api.exception.SinaqAssertionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class NetworkntSchemaValidatorTest {

    private final NetworkntSchemaValidator validator = new NetworkntSchemaValidator();

    @Test
    void validatesMatchingJson() {
        String schema = """
                {"type":"object","properties":{"id":{"type":"integer"}},"required":["id"]}
                """;
        assertThatCode(() -> validator.validate("{\"id\":1}", schema)).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidJson() {
        String schema = """
                {"type":"object","properties":{"id":{"type":"integer"}},"required":["id"]}
                """;
        assertThatThrownBy(() -> validator.validate("{\"id\":\"x\"}", schema))
                .isInstanceOf(SinaqAssertionException.class);
    }
}
