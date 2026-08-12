package io.sinaq.api.conformance;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ConformanceFixtureTest {

    @Test
    void exportsJsonSuite() {
        String json = ConformanceFixture.exportSuite();
        assertThat(json).contains("\"schemaVersion\":\"0.4\"");
        assertThat(json).contains("retryOn503");
    }
}
