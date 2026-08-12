package io.sinaq.api.events;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class EventPublisherTest {

    private static ReportEvent event(EventType type) {
        return ReportEvent.builder(type, "exec-1").requestId("req-1").build();
    }

    @Test
    void deliversToAllListenersInOrder() {
        EventPublisher publisher = EventPublisher.createSync();
        List<String> seen = new ArrayList<>();
        publisher.register(e -> seen.add("a:" + e.type()));
        publisher.register(e -> seen.add("b:" + e.type()));
        publisher.publish(event(EventType.REQUEST_STARTED));
        assertThat(seen).containsExactly("a:REQUEST_STARTED", "b:REQUEST_STARTED");
    }

    @Test
    void listenerFailureIsIsolated() {
        EventPublisher publisher = EventPublisher.createSync();
        AtomicInteger delivered = new AtomicInteger();
        publisher.register(e -> { throw new IllegalStateException("broken reporter"); });
        publisher.register(e -> delivered.incrementAndGet());
        assertThatCode(() -> publisher.publish(event(EventType.ASSERTION_FAILED)))
                .doesNotThrowAnyException();
        assertThat(delivered.get()).isEqualTo(1);
    }

    @Test
    void unregisterStopsDelivery() {
        EventPublisher publisher = EventPublisher.createSync();
        AtomicInteger count = new AtomicInteger();
        EventListener l = e -> count.incrementAndGet();
        publisher.register(l);
        publisher.publish(event(EventType.TEST_STARTED));
        publisher.unregister(l);
        publisher.publish(event(EventType.TEST_FINISHED));
        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    void concurrentPublishIsSafe() throws Exception {
        EventPublisher publisher = EventPublisher.createSync();
        AtomicInteger count = new AtomicInteger();
        publisher.register(e -> count.incrementAndGet());
        int threads = 20;
        int perThread = 100;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                start.await();
                for (int j = 0; j < perThread; j++) {
                    publisher.publish(event(EventType.REQUEST_SENT));
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        assertThat(count.get()).isEqualTo(threads * perThread);
    }

    @Test
    void eventEnvelopeIsImmutableAndComplete() {
        ReportEvent e = ReportEvent.builder(EventType.RESPONSE_RECEIVED, "exec-9")
                .testId("t1").requestId("r1").correlationId("c1")
                .payload(java.util.Map.of("status", 200))
                .build();
        assertThat(e.executionId()).isEqualTo("exec-9");
        assertThat(e.testId()).contains("t1");
        assertThat(e.payload()).containsEntry("status", 200);
        assertThatThrownBy(() -> e.payload().put("x", 1))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ReportEvent.builder(null, "e"))
                .isInstanceOf(NullPointerException.class);
    }
}
