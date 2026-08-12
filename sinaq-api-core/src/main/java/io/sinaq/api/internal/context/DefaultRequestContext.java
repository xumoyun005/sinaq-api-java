package io.sinaq.api.internal.context;

import io.sinaq.api.context.RequestContext;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * INTERNAL — not part of the public API and may change without notice.
 * Default immutable {@link RequestContext} implementation.
 */
public record DefaultRequestContext(String requestId,
                                    String correlationIdValue,
                                    Map<String, Object> metadataMap) implements RequestContext {

    public DefaultRequestContext {
        Objects.requireNonNull(requestId, "requestId");
        metadataMap = metadataMap == null ? Map.of() : Map.copyOf(metadataMap);
    }

    public static DefaultRequestContext create() {
        return new DefaultRequestContext(UUID.randomUUID().toString(), null, Map.of());
    }

    public static DefaultRequestContext withCorrelationId(String correlationId) {
        return new DefaultRequestContext(UUID.randomUUID().toString(), correlationId, Map.of());
    }

    @Override
    public Optional<String> correlationId() {
        return Optional.ofNullable(correlationIdValue);
    }

    @Override
    public Map<String, Object> metadata() {
        return metadataMap;
    }
}
