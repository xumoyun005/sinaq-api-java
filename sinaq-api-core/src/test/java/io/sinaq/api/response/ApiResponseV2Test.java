package io.sinaq.api.response;

import io.sinaq.api.assertion.SchemaValidator;
import io.sinaq.api.client.Sinaq;
import io.sinaq.api.support.StubHttpEngine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ApiResponseV2Test {

    @Test
    void expectSchemaUsesValidator() {
        StubHttpEngine engine = new StubHttpEngine();
        engine.respondJson(200, "{\"id\":1}");
        SchemaValidator validator = (body, schema) -> assertThat(body).contains("id");
        var api = Sinaq.emptyClient()
                .baseUrl("https://api.example.com")
                .engine(engine)
                .schemaValidator(validator)
                .build();
        api.get("/x").execute().expectSchema("{}");
    }
}
