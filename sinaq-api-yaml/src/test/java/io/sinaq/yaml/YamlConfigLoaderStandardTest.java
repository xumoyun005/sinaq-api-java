package io.sinaq.yaml;

import io.sinaq.api.config.SinaqConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class YamlConfigLoaderStandardTest {

    @Test
    void standardWithYamlLoadsFromClasspath() {
        SinaqConfig c = YamlConfigLoader.standardWithYaml().load();
        assertThat(c.environment()).isEqualTo("int");
        assertThat(c.baseUrl()).contains("https://int-yaml.example.com");
    }
}
