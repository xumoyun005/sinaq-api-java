package io.sinaq.api.events;

import io.sinaq.api.internal.events.SyncEventPublisher;

/**
 * Publishes framework events to registered listeners (spec §22).
 *
 * <p>Implementations must be thread-safe: parallel tests publish concurrently.
 * The core never depends on any report UI — reporters subscribe here.</p>
 */
public interface EventPublisher {

    /** Delivers the event to all listeners, isolating listener failures. */
    void publish(ReportEvent event);

    void register(EventListener listener);

    void unregister(EventListener listener);

    /**
     * Default synchronous publisher: events are delivered in the publishing
     * thread, preserving per-request ordering.
     */
    static EventPublisher createSync() {
        return new SyncEventPublisher();
    }
}
