package io.sinaq.api.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class RetryPolicyTest {

    @Test
    void noneIsDisabled() {
        assertThat(RetryPolicy.none().enabled()).isFalse();
        assertThat(RetryPolicy.none().maxAttempts()).isEqualTo(1);
    }

    @Test
    void builderProducesExplicitPolicy() {
        RetryPolicy p = RetryPolicy.builder()
                .maxAttempts(4).backoff(Duration.ofMillis(50)).onTimeout().onStatus(502, 503).build();
        assertThat(p.enabled()).isTrue();
        assertThat(p.onTimeout()).isTrue();
        assertThat(p.onTransportError()).isFalse();
        assertThat(p.onStatusCodes()).containsExactlyInAnyOrder(502, 503);
    }

    @Test
    void validation() {
        assertThatThrownBy(() -> new RetryPolicy(0, Duration.ZERO, false, false, java.util.Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
