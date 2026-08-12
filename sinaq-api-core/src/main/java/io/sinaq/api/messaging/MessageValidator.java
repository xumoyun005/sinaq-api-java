package io.sinaq.api.messaging;

import java.time.Duration;

/**
 * Async message validation SPI (V4). Implementations: in-memory test bus, Kafka/Rabbit adapters.
 */
public interface MessageValidator {

    /** Waits until a message matching the expectation arrives on the topic. */
    MessageRecord awaitMessage(String topic, Duration timeout, MessageExpectation expectation);

    /** Publishes a message (test/dev implementations only). */
    default void publish(String topic, String body) {
        publish(topic, body, "");
    }

    default void publish(String topic, String body, String header) {
        throw new UnsupportedOperationException("publish not supported by " + getClass().getSimpleName());
    }
}
