package io.sinaq.api.messaging;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class InMemoryMessageBusTest {

    @Test
    void publishesAndAwaitsMessage() {
        InMemoryMessageBus bus = new InMemoryMessageBus();
        Thread publisher = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}
            bus.publish("orders", "{\"id\":1}", "event=created");
        });
        publisher.start();
        MessageRecord msg = bus.awaitMessage("orders", Duration.ofSeconds(2),
                MessageExpectation.builder().bodyContains("\"id\":1").headerEquals("event=created").build());
        assertThat(msg.topic()).isEqualTo("orders");
    }
}
