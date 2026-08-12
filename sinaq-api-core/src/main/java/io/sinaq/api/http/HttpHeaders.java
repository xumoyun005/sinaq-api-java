package io.sinaq.api.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable, case-insensitive, multi-value HTTP header collection.
 *
 * <p>Lookup is case-insensitive per RFC 9110; the original casing of the first
 * occurrence of a name is preserved for output. Thread-safe.</p>
 */
public final class HttpHeaders {

    private static final HttpHeaders EMPTY = new HttpHeaders(new LinkedHashMap<>());

    /** lower-cased name -> (original name, values) */
    private final Map<String, Entry> entries;

    private record Entry(String originalName, List<String> values) {}

    private HttpHeaders(Map<String, Entry> entries) {
        this.entries = entries;
    }

    public static HttpHeaders empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Convenience factory for single-value headers. */
    public static HttpHeaders of(Map<String, String> singleValued) {
        Builder b = builder();
        singleValued.forEach(b::add);
        return b.build();
    }

    /** First value of the header, if present. */
    public Optional<String> first(String name) {
        Entry e = entries.get(key(name));
        return e == null ? Optional.empty() : Optional.of(e.values().get(0));
    }

    /** All values of the header; empty list if absent. Unmodifiable. */
    public List<String> all(String name) {
        Entry e = entries.get(key(name));
        return e == null ? List.of() : Collections.unmodifiableList(e.values());
    }

    public boolean contains(String name) {
        return entries.containsKey(key(name));
    }

    /** Header names in insertion order, original casing. Unmodifiable. */
    public Set<String> names() {
        Set<String> names = new LinkedHashSet<>();
        entries.values().forEach(e -> names.add(e.originalName()));
        return Collections.unmodifiableSet(names);
    }

    /** Number of distinct header names. */
    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Snapshot as name -> values map (original casing, insertion order). Unmodifiable. */
    public Map<String, List<String>> asMap() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        entries.values().forEach(e ->
                out.put(e.originalName(), Collections.unmodifiableList(e.values())));
        return Collections.unmodifiableMap(out);
    }

    /** Builder pre-populated with this instance's headers. */
    public Builder toBuilder() {
        Builder b = new Builder();
        entries.values().forEach(e -> e.values().forEach(v -> b.add(e.originalName(), v)));
        return b;
    }

    private static String key(String name) {
        return Objects.requireNonNull(name, "header name").toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "HttpHeaders" + asMap();
    }

    /** Mutable builder. NOT thread-safe; confine to one thread. */
    public static final class Builder {

        private final Map<String, Entry> entries = new LinkedHashMap<>();

        private Builder() {}

        /** Adds a value, keeping existing values of the same header. */
        public Builder add(String name, String value) {
            Objects.requireNonNull(value, "header value");
            String k = key(name);
            entries.computeIfAbsent(k, unused -> new Entry(name, new ArrayList<>()))
                   .values().add(value);
            return this;
        }

        /** Replaces all values of the header with a single value. */
        public Builder set(String name, String value) {
            Objects.requireNonNull(value, "header value");
            List<String> values = new ArrayList<>();
            values.add(value);
            entries.put(key(name), new Entry(name, values));
            return this;
        }

        public Builder remove(String name) {
            entries.remove(key(name));
            return this;
        }

        public HttpHeaders build() {
            Map<String, Entry> copy = new LinkedHashMap<>();
            entries.forEach((k, e) -> copy.put(k, new Entry(e.originalName(), List.copyOf(e.values()))));
            return new HttpHeaders(copy);
        }
    }
}
