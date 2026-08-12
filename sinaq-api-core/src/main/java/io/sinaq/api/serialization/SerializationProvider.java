package io.sinaq.api.serialization;

import java.lang.reflect.Type;

/**
 * Serialization SPI (spec §15). The core never hard-codes a JSON library.
 *
 * <p>Default: the dependency-free built-in provider (Map/List/records/primitives).
 * For arbitrary POJOs plug in the {@code sinaq-api-jackson} module or your own
 * implementation. Implementations must be thread-safe.</p>
 */
public interface SerializationProvider {

    String serialize(Object value);

    <T> T deserialize(String content, Class<T> type);

    <T> T deserialize(String content, Type type);

    /** The dependency-free default provider. */
    static SerializationProvider builtIn() {
        return io.sinaq.api.internal.json.BuiltInSerializationProvider.INSTANCE;
    }
}
