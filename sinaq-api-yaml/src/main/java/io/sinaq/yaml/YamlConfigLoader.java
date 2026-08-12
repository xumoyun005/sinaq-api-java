package io.sinaq.yaml;

import io.sinaq.api.config.ConfigKeys;
import io.sinaq.api.config.ConfigLoader;
import io.sinaq.api.exception.SinaqConfigurationException;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Loads {@code application.yml} / {@code application-{env}.yml} into {@link Properties} (V2).
 */
public final class YamlConfigLoader {

  private YamlConfigLoader() {}

  public static Properties loadClasspath(String resourceName) {
    Properties props = new Properties();
    try (InputStream in = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream(resourceName)) {
      if (in == null) {
        return props;
      }
      Object root = new Yaml().load(in);
      if (root instanceof Map<?, ?> map) {
        flatten("", map, props);
      }
    } catch (IOException e) {
      throw new SinaqConfigurationException("Failed to read YAML: " + resourceName, e);
    }
    return props;
  }

  @SuppressWarnings("unchecked")
  private static void flatten(String prefix, Map<?, ?> map, Properties props) {
    map.forEach((k, v) -> {
      String segment = String.valueOf(k);
      String key = prefix.isEmpty() ? segment : prefix + "." + segment;
      if (v instanceof Map<?, ?> nested) {
        flatten(key, nested, props);
      } else if (v != null) {
        props.setProperty(key, String.valueOf(v));
      }
    });
  }

  public static String baseYamlFile() {
    return "application.yml";
  }

  public static String envYamlFile(String env) {
    return String.format("application-%s.yml", env);
  }

  /** Classpath loader that maps properties file names to their YAML companions. */
  public static Properties yamlCompanion(String propertiesResourceName) {
    if (ConfigKeys.BASE_FILE.equals(propertiesResourceName)) {
      return loadClasspath(baseYamlFile());
    }
    String prefix = "application-";
    String suffix = ".properties";
    if (propertiesResourceName.startsWith(prefix) && propertiesResourceName.endsWith(suffix)) {
      String env = propertiesResourceName.substring(prefix.length(),
          propertiesResourceName.length() - suffix.length());
      return loadClasspath(envYamlFile(env));
    }
    return new Properties();
  }

  /** {@link ConfigLoader#standard()} with YAML companions on the classpath. */
  public static ConfigLoader standardWithYaml() {
    return ConfigLoader.standard().augmentClasspath(YamlConfigLoader::yamlCompanion);
  }
}
