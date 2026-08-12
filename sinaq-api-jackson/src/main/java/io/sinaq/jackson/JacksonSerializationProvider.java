package io.sinaq.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sinaq.api.exception.SinaqSerializationException;
import io.sinaq.api.serialization.SerializationProvider;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Jackson-backed {@link SerializationProvider} for arbitrary POJOs.
 * Usage: {@code Sinaq.client().serializationProvider(new JacksonSerializationProvider())}.
 * Thread-safe (ObjectMapper is thread-safe after configuration).
 */
public final class JacksonSerializationProvider implements SerializationProvider {

    private final ObjectMapper mapper;

    public JacksonSerializationProvider() {
        this(new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
    }

    public JacksonSerializationProvider(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public String serialize(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new SinaqSerializationException("Jackson serialization failed", e);
        }
    }

    @Override
    public <T> T deserialize(String content, Class<T> type) {
        try {
            return mapper.readValue(content, type);
        } catch (JsonProcessingException e) {
            throw new SinaqSerializationException("Jackson deserialization to " + type.getName() + " failed", e);
        }
    }

    @Override
    public <T> T deserialize(String content, Type type) {
        try {
            return mapper.readValue(content, mapper.getTypeFactory().constructType(type));
        } catch (JsonProcessingException e) {
            throw new SinaqSerializationException("Jackson deserialization to " + type + " failed", e);
        }
    }
}
