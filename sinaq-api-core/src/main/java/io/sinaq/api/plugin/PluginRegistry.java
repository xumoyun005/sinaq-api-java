package io.sinaq.api.plugin;

import io.sinaq.api.SinaqRuntime;
import io.sinaq.api.events.EventType;
import io.sinaq.api.events.ReportEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Global plugin registry (V2). Thread-safe; plugins are loaded at startup
 * or registered programmatically before client build.
 */
public final class PluginRegistry {

    private static final PluginRegistry GLOBAL = new PluginRegistry();
    private final CopyOnWriteArrayList<SinaqPlugin> plugins = new CopyOnWriteArrayList<>();

    public static PluginRegistry global() {
        return GLOBAL;
    }

    public void register(SinaqPlugin plugin) {
        plugins.addIfAbsent(plugin);
        SinaqRuntime.publisher().publish(ReportEvent.builder(
                        EventType.PLUGIN_LOADED, SinaqRuntime.executionContext().executionId())
                .payload(Map.of("pluginId", plugin.id()))
                .build());
    }

    public List<SinaqPlugin> plugins() {
        return List.copyOf(plugins);
    }

    public void clear() {
        plugins.clear();
    }
}
