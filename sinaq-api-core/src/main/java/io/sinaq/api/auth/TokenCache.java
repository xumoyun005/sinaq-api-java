package io.sinaq.api.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Thread-safe in-memory token cache with optional expiry (V2).
 */
public final class TokenCache {

    private final AtomicReference<Entry> entry = new AtomicReference<>();

    public Optional<String> get() {
        Entry e = entry.get();
        if (e == null) {
            return Optional.empty();
        }
        if (e.expiresAt != null && Instant.now().isAfter(e.expiresAt)) {
            entry.compareAndSet(e, null);
            return Optional.empty();
        }
        return Optional.of(e.token);
    }

    public void put(String token, Instant expiresAt) {
        entry.set(new Entry(Objects.requireNonNull(token), expiresAt));
    }

    public void put(String token) {
        put(token, null);
    }

    public void invalidate() {
        entry.set(null);
    }

    /** Returns cached token or fetches via supplier and caches the result. */
    public String getOrFetch(Supplier<String> fetcher, Instant expiresAt) {
        return get().orElseGet(() -> {
            String token = fetcher.get();
            put(token, expiresAt);
            return token;
        });
    }

    private record Entry(String token, Instant expiresAt) {}
}
