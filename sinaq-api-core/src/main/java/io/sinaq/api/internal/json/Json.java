package io.sinaq.api.internal.json;

import io.sinaq.api.exception.SinaqSerializationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * INTERNAL — minimal, dependency-free JSON parser.
 * Produces Map&lt;String,Object&gt; / List&lt;Object&gt; / String / Long / Double / Boolean / null.
 * Integral numbers become Long, others Double. Thread-safe (stateless entry point).
 */
public final class Json {

    private final String src;
    private int pos;

    private Json(String src) {
        this.src = src;
    }

    public static Object parse(String text) {
        if (text == null) {
            throw new SinaqSerializationException("Cannot parse null JSON");
        }
        Json p = new Json(text);
        p.skipWs();
        Object value = p.readValue();
        p.skipWs();
        if (p.pos < p.src.length()) {
            throw p.error("Unexpected trailing content");
        }
        return value;
    }

    private Object readValue() {
        if (pos >= src.length()) {
            throw error("Unexpected end of JSON");
        }
        char c = src.charAt(pos);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject() {
        expect('{');
        Map<String, Object> map = new LinkedHashMap<>();
        skipWs();
        if (peek() == '}') { pos++; return map; }
        while (true) {
            skipWs();
            String key = readString();
            skipWs();
            expect(':');
            skipWs();
            map.put(key, readValue());
            skipWs();
            char c = next();
            if (c == '}') return map;
            if (c != ',') throw error("Expected ',' or '}' in object");
        }
    }

    private List<Object> readArray() {
        expect('[');
        List<Object> list = new ArrayList<>();
        skipWs();
        if (peek() == ']') { pos++; return list; }
        while (true) {
            skipWs();
            list.add(readValue());
            skipWs();
            char c = next();
            if (c == ']') return list;
            if (c != ',') throw error("Expected ',' or ']' in array");
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') return sb.toString();
            if (c == '\\') {
                char e = next();
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > src.length()) throw error("Bad \\u escape");
                        sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> throw error("Bad escape: \\" + e);
                }
            } else {
                sb.append(c);
            }
        }
    }

    private Object readNumber() {
        int start = pos;
        if (peek() == '-') pos++;
        while (pos < src.length() && "0123456789.eE+-".indexOf(src.charAt(pos)) >= 0) pos++;
        String raw = src.substring(start, pos);
        try {
            if (raw.indexOf('.') < 0 && raw.indexOf('e') < 0 && raw.indexOf('E') < 0) {
                try {
                    return Long.parseLong(raw);
                } catch (NumberFormatException overflow) {
                    return Double.parseDouble(raw);
                }
            }
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw error("Invalid number: " + raw);
        }
    }

    private Object readLiteral(String literal, Object value) {
        if (!src.startsWith(literal, pos)) throw error("Invalid literal");
        pos += literal.length();
        return value;
    }

    private void skipWs() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private char peek() {
        if (pos >= src.length()) throw error("Unexpected end of JSON");
        return src.charAt(pos);
    }

    private char next() {
        if (pos >= src.length()) throw error("Unexpected end of JSON");
        return src.charAt(pos++);
    }

    private void expect(char c) {
        if (next() != c) throw error("Expected '" + c + "'");
    }

    private SinaqSerializationException error(String message) {
        return new SinaqSerializationException("JSON parse error at position " + pos + ": " + message);
    }
}
