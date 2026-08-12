package io.sinaq.api.internal.json;

import io.sinaq.api.exception.SinaqSerializationException;
import io.sinaq.api.serialization.SerializationProvider;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * INTERNAL — dependency-free {@link SerializationProvider}.
 * Serializes Map/List/records/primitives; deserializes into Map/List/String/Number/Boolean.
 * Arbitrary POJO deserialization requires sinaq-api-jackson.
 */
public final class BuiltInSerializationProvider implements SerializationProvider {

    public static final BuiltInSerializationProvider INSTANCE = new BuiltInSerializationProvider();

    private BuiltInSerializationProvider() {}

    @Override
    public String serialize(Object value) {
        return JsonWriter.write(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String content, Class<T> type) {
        Object parsed = Json.parse(content);
        if (type == Object.class || type.isInstance(parsed)
                || (type == Map.class && parsed instanceof Map)
                || (type == List.class && parsed instanceof List)) {
            return (T) parsed;
        }
        throw new SinaqSerializationException(
                "Built-in provider cannot map JSON to " + type.getName()
                + ". Add the sinaq-api-jackson module or a custom SerializationProvider.");
    }

    @Override
    public <T> T deserialize(String content, Type type) {
        if (type instanceof Class<?> c) {
            @SuppressWarnings("unchecked")
            T t = (T) deserialize(content, (Class<Object>) c);
            return t;
        }
        throw new SinaqSerializationException(
                "Built-in provider does not support generic type " + type
                + ". Add the sinaq-api-jackson module.");
    }
}
