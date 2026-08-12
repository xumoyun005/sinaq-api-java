package io.sinaq.api.graphql;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class GraphQlTest {

    @Test
    void buildsQueryAndVariables() {
        Map<String, Object> body = GraphQl.query("{ hello }", Map.of("id", 1));
        assertThat(body.get("query")).isEqualTo("{ hello }");
        assertThat(body.get("variables")).isEqualTo(Map.of("id", 1));
    }
}
