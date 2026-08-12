package io.sinaq.api.client;

import io.sinaq.api.config.ConfigLoader;
import io.sinaq.api.config.EnvironmentProfile;

/**
 * Entry point of the Sinaq DSL.
 */
public final class Sinaq {

    private Sinaq() {}

    /** Client builder pre-loaded from external configuration (files/env/props). */
    public static ApiClient.Builder client() {
        return new ApiClient.Builder().config(ConfigLoader.standard().load());
    }

    /** Client builder with library defaults only (no external config lookup). */
    public static ApiClient.Builder emptyClient() {
        return new ApiClient.Builder();
    }

    /** Client builder for a named environment profile (V2). */
    public static ApiClient.Builder client(EnvironmentProfile profile) {
        return client().profile(profile);
    }
}
