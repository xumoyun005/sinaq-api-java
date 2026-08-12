package io.sinaq.api.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class HttpQueryParamsTest {

    @Test
    void preservesInsertionOrderAndMultiValues() {
        HttpQueryParams q = HttpQueryParams.builder()
                .add("page", 1)
                .add("size", 20)
                .add("tag", "a")
                .add("tag", "b")
                .build();
        assertThat(q.asList()).extracting(HttpQueryParams.Param::name)
                .containsExactly("page", "size", "tag", "tag");
        assertThat(q.all("tag")).containsExactly("a", "b");
        assertThat(q.first("page")).contains("1");
    }

    @Test
    void emptyIsSingletonAndSafe() {
        assertThat(HttpQueryParams.builder().build()).isSameAs(HttpQueryParams.empty());
        assertThat(HttpQueryParams.empty().first("x")).isEmpty();
    }

    @Test
    void builtInstanceIsSnapshot() {
        HttpQueryParams.Builder b = HttpQueryParams.builder().add("a", 1);
        HttpQueryParams q = b.build();
        b.add("b", 2);
        assertThat(q.size()).isEqualTo(1);
    }

    @Test
    void nullNameRejected() {
        assertThatThrownBy(() -> HttpQueryParams.builder().add(null, "v"))
                .isInstanceOf(NullPointerException.class);
    }
}
