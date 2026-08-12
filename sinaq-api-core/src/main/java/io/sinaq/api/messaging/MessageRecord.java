package io.sinaq.api.messaging;

import java.time.Instant;
import java.util.Objects;

/** One captured message (V4). */
public record MessageRecord(String topic, String body, String header, Instant timestamp) {
    public MessageRecord {
        Objects.requireNonNull(topic, "topic");
        body = body == null ? "" : body;
        header = header == null ? "" : header;
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }
}
