package io.sinaq.api.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TestDataTest {

    @Test
    void generatesUniqueValues() {
        assertThat(TestData.uniqueId()).isNotBlank();
        assertThat(TestData.randomEmail()).contains("@example.test");
        assertThat(TestData.randomPhone()).startsWith("+99890");
        assertThat(TestData.randomDigits(4)).hasSize(4);
        assertThat(TestData.todayIso()).matches("\\d{4}-\\d{2}-\\d{2}");
    }
}
