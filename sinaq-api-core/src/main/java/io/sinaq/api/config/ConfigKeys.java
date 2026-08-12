package io.sinaq.api.config;

/** Well-known Sinaq configuration keys (properties / -D system properties). */
public final class ConfigKeys {

    public static final String BASE_URL        = "sinaq.baseUrl";
    public static final String ENVIRONMENT     = "sinaq.env";
    /** Connect timeout in milliseconds. */
    public static final String TIMEOUT_CONNECT = "sinaq.timeout.connect";
    /** Read timeout in milliseconds. */
    public static final String TIMEOUT_READ    = "sinaq.timeout.read";
    /** Default retry max attempts (transport only). 1 = disabled. */
    public static final String RETRY_MAX_ATTEMPTS = "sinaq.retry.maxAttempts";
    /** Default retry backoff in milliseconds. */
    public static final String RETRY_BACKOFF_MS   = "sinaq.retry.backoffMs";
    /** Logging level: OFF, WARN, INFO, DEBUG. */
    public static final String LOGGING_LEVEL      = "sinaq.logging.level";

    /** Base config file on the classpath. */
    public static final String BASE_FILE = "application.properties";
    /** Environment-specific file pattern, e.g. application-int.properties. */
    public static final String ENV_FILE_PATTERN = "application-%s.properties";

    private ConfigKeys() {}

    /** Env-var form of a key: {@code sinaq.timeout.connect -> SINAQ_TIMEOUT_CONNECT}. */
    public static String toEnvVar(String key) {
        return key.replace('.', '_').toUpperCase(java.util.Locale.ROOT);
    }
}
