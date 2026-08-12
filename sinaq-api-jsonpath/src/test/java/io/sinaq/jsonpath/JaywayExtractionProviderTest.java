package io.sinaq.jsonpath;

import io.sinaq.api.internal.json.Json;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JaywayExtractionProviderTest {

    private final JaywayExtractionProvider provider = new JaywayExtractionProvider();

    @Test
    void extractsNestedPathsAndWildcards() {
        Object root = Json.parse("{\"cards\":[{\"uuid\":\"u-1\"},{\"uuid\":\"u-2\"}]}");
        assertThat(provider.extract(root, "$.cards[0].uuid")).isEqualTo("u-1");
        List<String> uuids = (List<String>) provider.extract(root, "$.cards[*].uuid");
        assertThat(uuids).containsExactlyElementsOf(List.of("u-1", "u-2"));
    }
}
