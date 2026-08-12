package io.sinaq.api.messaging;

import io.sinaq.api.exception.SinaqTimeoutException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory message bus for tests and local dev (V4).
 */
public final class InMemoryMessageBus implements MessageValidator {

    private final Map<String, List<MessageRecord>> topics = new ConcurrentHashMap<>();

    @Override
    public MessageRecord awaitMessage(String topic, Duration timeout, MessageExpectation expectation) {
        Instant deadline = Instant.now().plus(timeout != null ? timeout : Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            for (MessageRecord msg : snapshot(topic)) {
                try {
                    expectation.verify(msg);
                    return msg;
                } catch (RuntimeException ignored) {
                    // try next message
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SinaqTimeoutException("Interrupted waiting for message on " + topic, e);
            }
        }
        throw new SinaqTimeoutException("No matching message on topic '" + topic + "' within " + timeout);
    }

    @Override
    public void publish(String topic, String body, String header) {
        topics.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>())
                .add(new MessageRecord(topic, body, header, Instant.now()));
    }

    public List<MessageRecord> snapshot(String topic) {
        return List.copyOf(topics.getOrDefault(topic, List.of()));
    }

    public void clear() {
        topics.clear();
    }
}
