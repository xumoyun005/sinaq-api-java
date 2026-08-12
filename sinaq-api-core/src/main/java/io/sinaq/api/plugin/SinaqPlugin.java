package io.sinaq.api.plugin;

import io.sinaq.api.client.ApiClient;

/**
 * Extension point for V2 plugin architecture. Plugins register capabilities
 * when an {@link ApiClient} is built.
 */
public interface SinaqPlugin {

    /** Stable plugin id, e.g. {@code "jsonpath"}. */
    String id();

    /** Called once per client build; may customize builder state. */
    default void onClientBuild(ApiClient.Builder builder) {}
}
