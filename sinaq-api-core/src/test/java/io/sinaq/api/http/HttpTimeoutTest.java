package io.sinaq.api.http;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class HttpTimeoutTest {

    @Test
    void defaultsAreSane() {
        HttpTimeout t = HttpTimeout.defaults();
        assertThat(t.connect()).isEqualTo(Duration.ofSeconds(10));
        assertThat(t.read()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void rejectsNonPositiveDurations() {
        assertThatThrownBy(() -> HttpTimeout.of(Duration.ZERO, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HttpTimeout.of(Duration.ofSeconds(1), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HttpTimeout.of(null, Duration.ofSeconds(1)))
                .isInstanceOf(NullPointerException.class);
    }
}
