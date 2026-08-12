package io.sinaq.api.cache;

import io.sinaq.api.http.HttpResponse;
import io.sinaq.api.http.ImmutableHttpResponse;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory response cache (V4). Thread-safe; keyed by caller-provided cache key.
 */
public final class ResponseCache {

    private final Map<String, HttpResponse> store = new ConcurrentHashMap<>();

    public HttpResponse get(String key) {
        if (key == null) {
            return null;
        }
        HttpResponse cached = store.get(key);
        return cached == null ? null : ImmutableHttpResponse.copyOf(cached);
    }

    public void put(String key, HttpResponse response) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(response, "response");
        store.put(key, ImmutableHttpResponse.copyOf(response));
    }

    public void invalidate(String key) {
        if (key != null) {
            store.remove(key);
        }
    }

    public void clear() {
        store.clear();
    }

    public int size() {
        return store.size();
    }
}
