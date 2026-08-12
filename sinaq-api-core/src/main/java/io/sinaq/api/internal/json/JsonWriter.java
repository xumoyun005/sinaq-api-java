package io.sinaq.api.internal.json;

import io.sinaq.api.exception.SinaqSerializationException;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Map;

/**
 * INTERNAL — minimal, dependency-free JSON writer.
 * Supports null, String, Number, Boolean, Map, Iterable, arrays and Java records.
 * Arbitrary POJOs need a real SerializationProvider (e.g. sinaq-api-jackson).
 */
public final class JsonWriter {

    private JsonWriter() {}

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, sb);
        return sb.toString();
    }

    private static void writeValue(Object v, StringBuilder sb) {
        switch (v) {
            case null -> sb.append("null");
            case String s -> writeString(s, sb);
            case Boolean b -> sb.append(b);
            case Number n -> sb.append(n);
            case Map<?, ?> m -> {
                sb.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (!first) sb.append(',');
                    first = false;
                    writeString(String.valueOf(e.getKey()), sb);
                    sb.append(':');
                    writeValue(e.getValue(), sb);
                }
                sb.append('}');
            }
            case Iterable<?> it -> {
                sb.append('[');
                boolean first = true;
                for (Object e : it) {
                    if (!first) sb.append(',');
                    first = false;
                    writeValue(e, sb);
                }
                sb.append(']');
            }
            case Object[] arr -> writeValue(List.of(arr), sb);
            default -> {
                if (v.getClass().isRecord()) {
                    writeRecord(v, sb);
                } else {
                    throw new SinaqSerializationException(
                            "Built-in serializer supports Map/List/records/primitives only; got "
                            + v.getClass().getName()
                            + ". Add the sinaq-api-jackson module or a custom SerializationProvider.");
                }
            }
        }
    }

    private static void writeRecord(Object rec, StringBuilder sb) {
        sb.append('{');
        RecordComponent[] components = rec.getClass().getRecordComponents();
        for (int i = 0; i < components.length; i++) {
            if (i > 0) sb.append(',');
            writeString(components[i].getName(), sb);
            sb.append(':');
            try {
                var accessor = components[i].getAccessor();
                if (!accessor.trySetAccessible()) {
                    throw new SinaqSerializationException(
                            "Cannot access record component " + components[i].getName());
                }
                writeValue(accessor.invoke(rec), sb);
            } catch (ReflectiveOperationException e) {
                throw new SinaqSerializationException(
                        "Failed to read record component " + components[i].getName(), e);
            }
        }
        sb.append('}');
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }
}
