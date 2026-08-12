package io.sinaq.api.http;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class HttpHeadersTest {

    @Test
    void lookupIsCaseInsensitive() {
        HttpHeaders h = HttpHeaders.builder().add("X-App-Version", "1.0.3").build();
        assertThat(h.first("x-app-version")).contains("1.0.3");
        assertThat(h.first("X-APP-VERSION")).contains("1.0.3");
        assertThat(h.contains("x-App-Version")).isTrue();
    }

    @Test
    void preservesOriginalCasingOfFirstOccurrence() {
        HttpHeaders h = HttpHeaders.builder()
                .add("Content-Type", "application/json")
                .add("content-type", "ignored-casing")
                .build();
        assertThat(h.names()).containsExactly("Content-Type");
        assertThat(h.all("CONTENT-TYPE")).containsExactly("application/json", "ignored-casing");
    }

    @Test
    void addKeepsMultipleValues_setReplacesThem() {
        HttpHeaders h = HttpHeaders.builder()
                .add("Accept", "application/json")
                .add("Accept", "text/plain")
                .set("Accept", "*/*")
                .build();
        assertThat(h.all("Accept")).containsExactly("*/*");
    }

    @Test
    void builtInstanceIsImmutableSnapshot() {
        HttpHeaders.Builder b = HttpHeaders.builder().add("A", "1");
        HttpHeaders h = b.build();
        b.add("A", "2").add("B", "3");
        assertThat(h.all("A")).containsExactly("1");
        assertThat(h.contains("B")).isFalse();
    }

    @Test
    void exposedCollectionsAreUnmodifiable() {
        HttpHeaders h = HttpHeaders.builder().add("A", "1").build();
        List<String> values = h.all("A");
        Map<String, List<String>> map = h.asMap();
        assertThatThrownBy(() -> values.add("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> map.put("B", List.of())).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void emptyAndMissingBehaviour() {
        assertThat(HttpHeaders.empty().isEmpty()).isTrue();
        assertThat(HttpHeaders.empty().first("nope")).isEmpty();
        assertThat(HttpHeaders.empty().all("nope")).isEmpty();
    }

    @Test
    void toBuilderRoundTrips() {
        HttpHeaders h = HttpHeaders.builder().add("A", "1").add("A", "2").build();
        HttpHeaders copy = h.toBuilder().add("B", "3").build();
        assertThat(copy.all("A")).containsExactly("1", "2");
        assertThat(copy.first("B")).contains("3");
        assertThat(h.contains("B")).isFalse();
    }

    @Test
    void nullNameOrValueRejected() {
        HttpHeaders.Builder b = HttpHeaders.builder();
        assertThatThrownBy(() -> b.add(null, "v")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> b.add("n", null)).isInstanceOf(NullPointerException.class);
    }
}
