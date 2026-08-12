package io.sinaq.yaml;

import io.sinaq.api.config.ConfigLoader;
import io.sinaq.api.config.SinaqConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

class YamlConfigLoaderTest {

    @Test
    void flattensNestedYamlIntoDotKeys() {
        Properties props = YamlConfigLoader.loadClasspath("test-application.yml");
        assertThat(props.getProperty("sinaq.baseUrl")).isEqualTo("https://yaml.example.com");
        assertThat(props.getProperty("sinaq.timeout.connect")).isEqualTo("4000");
        assertThat(props.getProperty("sinaq.env")).isEqualTo("int");
    }

    @Test
    void yamlCompanionMapsPropertiesFileNames() {
        Properties base = YamlConfigLoader.yamlCompanion("application.properties");
        assertThat(base.getProperty("sinaq.baseUrl")).isEqualTo("https://yaml.example.com");

        Properties env = YamlConfigLoader.yamlCompanion("application-int.properties");
        assertThat(env.getProperty("sinaq.baseUrl")).isEqualTo("https://int-yaml.example.com");
    }

    @Test
    void augmentClasspathLetsPropertiesOverrideYaml() {
        Properties yaml = YamlConfigLoader.loadClasspath("test-application.yml");
        Properties props = new Properties();
        props.setProperty("sinaq.baseUrl", "https://props-win.example.com");
        SinaqConfig c = ConfigLoader.of(Map.of(), Map.of(), Map.of(
                "application.properties", props))
                .augmentClasspath(name -> "application.properties".equals(name) ? yaml : new Properties())
                .load();
        assertThat(c.baseUrl()).contains("https://props-win.example.com");
        assertThat(c.timeout().connect()).isEqualTo(Duration.ofMillis(4000));
    }
}
