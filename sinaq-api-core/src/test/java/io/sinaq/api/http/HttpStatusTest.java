package io.sinaq.api.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class HttpStatusTest {

    @Test
    void classHelpers() {
        assertThat(HttpStatus.of(102).isInformational()).isTrue();
        assertThat(HttpStatus.of(200).isSuccess()).isTrue();
        assertThat(HttpStatus.of(302).isRedirect()).isTrue();
        assertThat(HttpStatus.of(404).isClientError()).isTrue();
        assertThat(HttpStatus.of(500).isServerError()).isTrue();
        assertThat(HttpStatus.of(500).isError()).isTrue();
        assertThat(HttpStatus.of(201).isError()).isFalse();
    }

    @Test
    void rejectsOutOfRangeCodes() {
        assertThatThrownBy(() -> HttpStatus.of(99)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HttpStatus.of(600)).isInstanceOf(IllegalArgumentException.class);
    }
}
