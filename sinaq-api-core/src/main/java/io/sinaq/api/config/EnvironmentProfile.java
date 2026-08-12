package io.sinaq.api.config;

import java.util.Locale;

/**
 * Well-known environment profiles (V2 / spec §17).
 */
public enum EnvironmentProfile {
    DEV("dev"),
    TEST("test"),
    INT("int"),
    STAGE("stage"),
    PROD("prod");

    private final String id;

    EnvironmentProfile(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static EnvironmentProfile from(String value) {
        if (value == null || value.isBlank()) {
            return DEV;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (EnvironmentProfile p : values()) {
            if (p.id.equals(normalized)) {
                return p;
            }
        }
        throw new io.sinaq.api.exception.SinaqConfigurationException(
                "Unknown environment profile: " + value + ". Expected: dev, test, int, stage, prod");
    }
}
