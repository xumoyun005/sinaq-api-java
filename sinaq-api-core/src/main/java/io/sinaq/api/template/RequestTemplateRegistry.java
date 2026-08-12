package io.sinaq.api.template;

import io.sinaq.api.exception.SinaqConfigurationException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of {@link RequestTemplate} instances (V2).
 */
public final class RequestTemplateRegistry {

    private final ConcurrentHashMap<String, RequestTemplate> templates = new ConcurrentHashMap<>();

    public void register(RequestTemplate template) {
        templates.put(template.name(), template);
    }

    public RequestTemplate get(String name) {
        RequestTemplate t = templates.get(name);
        if (t == null) {
            throw new SinaqConfigurationException("Unknown request template: " + name);
        }
        return t;
    }

    public boolean contains(String name) {
        return templates.containsKey(name);
    }

    public Map<String, RequestTemplate> all() {
        return Map.copyOf(templates);
    }
}
