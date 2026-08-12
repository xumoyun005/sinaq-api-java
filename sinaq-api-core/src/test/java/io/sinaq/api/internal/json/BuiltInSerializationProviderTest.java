package io.sinaq.api.internal.json;

import io.sinaq.api.exception.SinaqSerializationException;
import io.sinaq.api.serialization.SerializationProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class BuiltInSerializationProviderTest {

    private final SerializationProvider provider = SerializationProvider.builtIn();

    @Test
    void roundTripsMapAndList() {
        String json = provider.serialize(Map.of("a", List.of(1, 2)));
        @SuppressWarnings("unchecked")
        Map<String, Object> map = provider.deserialize(json, Map.class);
        assertThat(map).containsKey("a");
    }

    @Test
    void rejectsUnsupportedTypes() {
        assertThatThrownBy(() -> provider.deserialize("{}", String.class))
                .isInstanceOf(SinaqSerializationException.class)
                .hasMessageContaining("sinaq-api-jackson");

        Type generic = new ParameterizedType() {
            @Override public Type[] getActualTypeArguments() { return new Type[]{String.class}; }
            @Override public Type getRawType() { return List.class; }
            @Override public Type getOwnerType() { return null; }
        };
        assertThatThrownBy(() -> provider.deserialize("[]", generic))
                .isInstanceOf(SinaqSerializationException.class);
    }
}
