package io.sinaq.api.internal.json;

import io.sinaq.api.exception.SinaqSerializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * INTERNAL — lightweight JSONPath subset used by the core in V1:
 * {@code $.a.b}, {@code $.items[0].id}, {@code $.cards[*].uuid}, {@code [0].x}.
 * Missing paths resolve to null (or are skipped inside a wildcard).
 * Full JSONPath arrives via the extraction SPI in V2 (ADR-004).
 */
public final class JsonPathLite {

    private JsonPathLite() {}

    /** Resolves the path against a parsed JSON tree. Wildcards return List. */
    public static Object extract(Object root, String path) {
        List<String> segments = tokenize(path);
        return resolve(root, segments, 0);
    }

    private static Object resolve(Object node, List<String> segments, int index) {
        if (index == segments.size() || node == null) {
            return node;
        }
        String segment = segments.get(index);
        if (segment.equals("*")) {
            if (!(node instanceof List<?> list)) {
                return null;
            }
            List<Object> out = new ArrayList<>();
            for (Object item : list) {
                Object resolved = resolve(item, segments, index + 1);
                if (resolved != null) {
                    out.add(resolved);
                }
            }
            return out;
        }
        if (segment.chars().allMatch(Character::isDigit) && node instanceof List<?> list) {
            int i = Integer.parseInt(segment);
            return i < list.size() ? resolve(list.get(i), segments, index + 1) : null;
        }
        if (node instanceof Map<?, ?> map) {
            return resolve(map.get(segment), segments, index + 1);
        }
        return null;
    }

    private static List<String> tokenize(String path) {
        if (path == null || path.isBlank()) {
            throw new SinaqSerializationException("Empty extraction path");
        }
        String p = path.trim();
        if (p.startsWith("$")) p = p.substring(1);
        if (p.startsWith(".")) p = p.substring(1);
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c == '.') {
                flush(current, segments);
            } else if (c == '[') {
                flush(current, segments);
                int close = p.indexOf(']', i);
                if (close < 0) throw new SinaqSerializationException("Unclosed '[' in path: " + path);
                segments.add(p.substring(i + 1, close).trim());
                i = close;
            } else {
                current.append(c);
            }
        }
        flush(current, segments);
        if (segments.isEmpty()) {
            throw new SinaqSerializationException("Empty extraction path: " + path);
        }
        return segments;
    }

    private static void flush(StringBuilder sb, List<String> segments) {
        if (!sb.isEmpty()) {
            segments.add(sb.toString());
            sb.setLength(0);
        }
    }
}
