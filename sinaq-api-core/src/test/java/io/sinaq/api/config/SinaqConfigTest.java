package io.sinaq.api.config;

import io.sinaq.api.exception.SinaqConfigurationException;
import io.sinaq.api.http.HttpTimeout;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class SinaqConfigTest {

    @Test
    void builderAndToBuilderLayering() {
        SinaqConfig global = SinaqConfig.builder()
                .baseUrl("https://api.example.com")
                .defaultHeader("X-App-Version", "1.0.0")
                .build();
        SinaqConfig client = global.toBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .defaultHeader("X-Client", "loans")
                .build();

        // override applied
        assertThat(client.timeout().connect()).isEqualTo(Duration.ofSeconds(3));
        // inherited values kept
        assertThat(client.baseUrl()).contains("https://api.example.com");
        assertThat(client.defaultHeaders().first("X-App-Version")).contains("1.0.0");
        assertThat(client.defaultHeaders().first("X-Client")).contains("loans");
        // original snapshot untouched
        assertThat(global.timeout()).isEqualTo(HttpTimeout.defaults());
        assertThat(global.defaultHeaders().contains("X-Client")).isFalse();
    }

    @Test
    void rejectsNonHttpBaseUrl() {
        assertThatThrownBy(() -> SinaqConfig.builder().baseUrl("not a url"))
                .isInstanceOf(SinaqConfigurationException.class);
        assertThatThrownBy(() -> SinaqConfig.builder().baseUrl("/relative"))
                .isInstanceOf(SinaqConfigurationException.class);
    }
}
