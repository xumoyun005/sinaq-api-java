package io.sinaq.api.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class HttpPathParamsTest {

    @Test
    void setAndGet() {
        HttpPathParams p = HttpPathParams.builder().set("id", 42).set("name", "ali").build();
        assertThat(p.get("id")).contains("42");
        assertThat(p.get("missing")).isEmpty();
        assertThat(p.asMap()).containsExactly(
                java.util.Map.entry("id", "42"), java.util.Map.entry("name", "ali"));
    }

    @Test
    void exposedMapIsUnmodifiable() {
        HttpPathParams p = HttpPathParams.builder().set("id", 1).build();
        assertThatThrownBy(() -> p.asMap().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void emptyIsSingleton() {
        assertThat(HttpPathParams.builder().build()).isSameAs(HttpPathParams.empty());
    }
}
