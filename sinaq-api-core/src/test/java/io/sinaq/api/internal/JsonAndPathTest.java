package io.sinaq.api.internal;

import io.sinaq.api.exception.SinaqSerializationException;
import io.sinaq.api.internal.json.Json;
import io.sinaq.api.internal.json.JsonPathLite;
import io.sinaq.api.internal.json.JsonWriter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JsonAndPathTest {

    @Test
    void parsesTypesCorrectly() {
        Object root = Json.parse("{\"s\":\"uz \\u00e9\",\"i\":5,\"d\":1.5,\"b\":true,\"n\":null,\"a\":[1,2]}");
        Map<?, ?> map = (Map<?, ?>) root;
        assertThat(map.get("s")).isEqualTo("uz é");
        assertThat(map.get("i")).isEqualTo(5L);
        assertThat(map.get("d")).isEqualTo(1.5d);
        assertThat(map.get("b")).isEqualTo(Boolean.TRUE);
        assertThat(map.get("n")).isNull();
        assertThat(map.get("a")).isEqualTo(List.of(1L, 2L));
    }

    @Test
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> Json.parse("{\"a\":1")).isInstanceOf(SinaqSerializationException.class);
        assertThatThrownBy(() -> Json.parse("{}extra")).isInstanceOf(SinaqSerializationException.class);
    }

    @Test
    void pathVariants() {
        Object root = Json.parse("{\"cards\":[{\"uuid\":\"u-1\"},{\"uuid\":\"u-2\"}],\"a\":{\"b\":7}}");
        assertThat(JsonPathLite.extract(root, "$.a.b")).isEqualTo(7L);
        assertThat(JsonPathLite.extract(root, "a.b")).isEqualTo(7L);
        assertThat(JsonPathLite.extract(root, "$.cards[1].uuid")).isEqualTo("u-2");
        assertThat(JsonPathLite.extract(root, "$.cards[*].uuid")).isEqualTo(List.of("u-1", "u-2"));
        assertThat(JsonPathLite.extract(root, "$.missing.deep")).isNull();
    }

    @Test
    void writerRoundTripsMapsListsRecords() {
        record User(String name, int age) {}
        String json = JsonWriter.write(Map.of("user", new User("Ali", 30)));
        Object back = Json.parse(json);
        assertThat(JsonPathLite.extract(back, "user.name")).isEqualTo("Ali");
        assertThat(JsonPathLite.extract(back, "user.age")).isEqualTo(30L);
        assertThat(JsonWriter.write(List.of(1, "x", true))).isEqualTo("[1,\"x\",true]");
    }

    @Test
    void writerRejectsArbitraryPojos() {
        assertThatThrownBy(() -> JsonWriter.write(new Object()))
                .isInstanceOf(SinaqSerializationException.class)
                .hasMessageContaining("sinaq-api-jackson");
    }
}
