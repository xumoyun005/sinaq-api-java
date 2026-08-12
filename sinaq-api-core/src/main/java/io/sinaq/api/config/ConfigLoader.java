package io.sinaq.api.config;

import io.sinaq.api.exception.SinaqConfigurationException;
import io.sinaq.api.http.HttpTimeout;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

/**
 * Loads a {@link SinaqConfig} snapshot from external sources (spec §16–17).
 *
 * <p>Precedence for every key (highest wins):</p>
 * <ol>
 *   <li>JVM system property        {@code -Dsinaq.baseUrl=...}</li>
 *   <li>environment variable       {@code SINAQ_BASE_URL=...}</li>
 *   <li>env-specific classpath file {@code application-<env>.properties}</li>
 *   <li>base classpath file        {@code application.properties}</li>
 * </ol>
 *
 * <p>The active environment itself is resolved first with the same precedence
 * (key {@code sinaq.env}); it then selects the env-specific file.
 * Credentials must never live in files — use env vars or system properties.</p>
 *
 * <p>Instances are immutable; lookups are injected for testability. Thread-safe.</p>
 */
public final class ConfigLoader {

    private final Function<String, String> systemProps;
    private final Function<String, String> envVars;
    private final Function<String, Properties> classpathLoader;

    private ConfigLoader(Function<String, String> systemProps,
                         Function<String, String> envVars,
                         Function<String, Properties> classpathLoader) {
        this.systemProps = systemProps;
        this.envVars = envVars;
        this.classpathLoader = classpathLoader;
    }

    /** Loader backed by the real JVM system properties, env vars and classpath. */
    public static ConfigLoader standard() {
        return new ConfigLoader(System::getProperty, System::getenv, ConfigLoader::loadClasspath);
    }

    /** Fully injected loader — intended for tests. */
    public static ConfigLoader of(Map<String, String> systemProps,
                                  Map<String, String> envVars,
                                  Map<String, Properties> classpathFiles) {
        Objects.requireNonNull(systemProps);
        Objects.requireNonNull(envVars);
        Objects.requireNonNull(classpathFiles);
        return new ConfigLoader(systemProps::get, envVars::get,
                name -> classpathFiles.getOrDefault(name, new Properties()));
    }

    /**
     * Merges an additional classpath loader (e.g. YAML). Values from the primary loader
     * ({@code application.properties}) win over the overlay ({@code application.yml}).
     */
    public ConfigLoader augmentClasspath(Function<String, Properties> overlay) {
        Objects.requireNonNull(overlay, "overlay");
        return new ConfigLoader(systemProps, envVars, name -> {
            Properties overlayProps = overlay.apply(name);
            Properties primary = classpathLoader.apply(name);
            return merge(overlayProps, primary);
        });
    }

    private static Properties merge(Properties lowerPriority, Properties higherPriority) {
        Properties merged = new Properties();
        if (lowerPriority != null) {
            merged.putAll(lowerPriority);
        }
        if (higherPriority != null) {
            merged.putAll(higherPriority);
        }
        return merged;
    }

    /** Resolves the snapshot. */
    public SinaqConfig load() {
        Properties base = classpathLoader.apply(ConfigKeys.BASE_FILE);

        String env = firstNonNull(
                resolve(ConfigKeys.ENVIRONMENT, base, null),
                SinaqConfig.DEFAULT_ENVIRONMENT);

        Properties envFile = SinaqConfig.DEFAULT_ENVIRONMENT.equals(env)
                ? new Properties()
                : classpathLoader.apply(String.format(ConfigKeys.ENV_FILE_PATTERN, env));

        SinaqConfig.Builder b = SinaqConfig.builder().environment(env);

        String baseUrl = resolve(ConfigKeys.BASE_URL, base, envFile);
        if (baseUrl != null) {
            b.baseUrl(baseUrl);
        }

        Duration connect = millis(ConfigKeys.TIMEOUT_CONNECT, resolve(ConfigKeys.TIMEOUT_CONNECT, base, envFile));
        Duration read = millis(ConfigKeys.TIMEOUT_READ, resolve(ConfigKeys.TIMEOUT_READ, base, envFile));
        if (connect != null || read != null) {
            HttpTimeout d = HttpTimeout.defaults();
            b.timeout(HttpTimeout.of(connect != null ? connect : d.connect(),
                                     read != null ? read : d.read()));
        }

        String retryMax = resolve(ConfigKeys.RETRY_MAX_ATTEMPTS, base, envFile);
        String retryBackoff = resolve(ConfigKeys.RETRY_BACKOFF_MS, base, envFile);
        if (retryMax != null) {
            RetryPolicy.Builder rb = RetryPolicy.builder()
                    .maxAttempts(Integer.parseInt(retryMax.trim()));
            if (retryBackoff != null) {
                rb.backoff(Duration.ofMillis(Long.parseLong(retryBackoff.trim())));
            }
            b.defaultRetry(rb.build());
        }
        return b.build();
    }

    /** system prop > env var > env file > base file; null when nowhere defined. */
    private String resolve(String key, Properties base, Properties envFile) {
        String v = systemProps.apply(key);
        if (v == null) {
            v = envVars.apply(ConfigKeys.toEnvVar(key));
        }
        if (v == null && envFile != null) {
            v = envFile.getProperty(key);
        }
        if (v == null && base != null) {
            v = base.getProperty(key);
        }
        return v;
    }

    private static Duration millis(String key, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Duration.ofMillis(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            throw new SinaqConfigurationException(key + " must be milliseconds, got: " + value, e);
        }
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    private static Properties loadClasspath(String name) {
        Properties p = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            throw new SinaqConfigurationException("Failed to read classpath resource: " + name, e);
        }
        return p;
    }
}
