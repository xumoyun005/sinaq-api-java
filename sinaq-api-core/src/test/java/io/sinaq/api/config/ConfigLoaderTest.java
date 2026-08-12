package io.sinaq.api.config;

import io.sinaq.api.exception.SinaqConfigurationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

class ConfigLoaderTest {

    private static Properties props(String... kv) {
        Properties p = new Properties();
        for (int i = 0; i < kv.length; i += 2) {
            p.setProperty(kv[i], kv[i + 1]);
        }
        return p;
    }

    @Test
    void baseFileValuesAreLoaded() {
        SinaqConfig c = ConfigLoader.of(Map.of(), Map.of(), Map.of(
                "application.properties",
                props("sinaq.baseUrl", "https://api.example.com",
                      "sinaq.timeout.connect", "5000",
                      "sinaq.timeout.read", "15000"))).load();
        assertThat(c.baseUrl()).contains("https://api.example.com");
        assertThat(c.timeout().connect()).isEqualTo(Duration.ofMillis(5000));
        assertThat(c.timeout().read()).isEqualTo(Duration.ofMillis(15000));
        assertThat(c.environment()).isEqualTo("default");
    }

    @Test
    void envFileOverridesBaseFile_andEnvIsSelectedFromBase() {
        SinaqConfig c = ConfigLoader.of(Map.of(), Map.of(), Map.of(
                "application.properties",
                props("sinaq.env", "int", "sinaq.baseUrl", "https://base.example.com"),
                "application-int.properties",
                props("sinaq.baseUrl", "https://int.example.com"))).load();
        assertThat(c.environment()).isEqualTo("int");
        assertThat(c.baseUrl()).contains("https://int.example.com");
    }

    @Test
    void envVarOverridesFiles() {
        SinaqConfig c = ConfigLoader.of(
                Map.of(),
                Map.of("SINAQ_BASEURL", "https://env.example.com"),
                Map.of("application.properties", props("sinaq.baseUrl", "https://file.example.com"))
        ).load();
        assertThat(c.baseUrl()).contains("https://env.example.com");
    }

    @Test
    void systemPropertyOverridesEverything() {
        SinaqConfig c = ConfigLoader.of(
                Map.of("sinaq.baseUrl", "https://sys.example.com", "sinaq.env", "stage"),
                Map.of("SINAQ_BASEURL", "https://env.example.com", "SINAQ_ENV", "int"),
                Map.of("application.properties", props("sinaq.baseUrl", "https://file.example.com"))
        ).load();
        assertThat(c.baseUrl()).contains("https://sys.example.com");
        assertThat(c.environment()).isEqualTo("stage");
    }

    @Test
    void invalidTimeoutFailsFast() {
        ConfigLoader loader = ConfigLoader.of(Map.of("sinaq.timeout.connect", "fast"), Map.of(), Map.of());
        assertThatThrownBy(loader::load)
                .isInstanceOf(SinaqConfigurationException.class)
                .hasMessageContaining("sinaq.timeout.connect");
    }

    @Test
    void invalidBaseUrlFailsFast() {
        ConfigLoader loader = ConfigLoader.of(Map.of("sinaq.baseUrl", "ftp://nope"), Map.of(), Map.of());
        assertThatThrownBy(loader::load).isInstanceOf(SinaqConfigurationException.class);
    }

    @Test
    void missingEverythingYieldsDefaults() {
        SinaqConfig c = ConfigLoader.of(Map.of(), Map.of(), Map.of()).load();
        assertThat(c.baseUrl()).isEmpty();
        assertThat(c.timeout()).isEqualTo(io.sinaq.api.http.HttpTimeout.defaults());
        assertThat(c.environment()).isEqualTo("default");
    }

    @Test
    void envVarKeyMapping() {
        assertThat(ConfigKeys.toEnvVar("sinaq.timeout.connect")).isEqualTo("SINAQ_TIMEOUT_CONNECT");
    }

    @Test
    void augmentClasspathMergesOverlayUnderPrimary() {
        Properties overlay = props("sinaq.baseUrl", "https://yaml.example.com",
                "sinaq.timeout.connect", "4000");
        Properties primary = props("sinaq.baseUrl", "https://props.example.com");
        SinaqConfig c = ConfigLoader.of(Map.of(), Map.of(), Map.of(
                "application.properties", primary))
                .augmentClasspath(name -> overlay)
                .load();
        assertThat(c.baseUrl()).contains("https://props.example.com");
        assertThat(c.timeout().connect()).isEqualTo(Duration.ofMillis(4000));
    }
}
