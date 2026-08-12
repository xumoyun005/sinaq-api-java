package io.sinaq.api.internal.events;

import io.sinaq.api.events.EventListener;
import io.sinaq.api.events.EventPublisher;
import io.sinaq.api.events.ReportEvent;
import io.sinaq.api.internal.log.SinaqLog;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * INTERNAL — default synchronous, thread-safe {@link EventPublisher}.
 * Listener exceptions are caught and logged; they never propagate to tests.
 */
public final class SyncEventPublisher implements EventPublisher {

    private final List<EventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void publish(ReportEvent event) {
        Objects.requireNonNull(event, "event");
        for (EventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException e) {
                SinaqLog.warn("Event listener " + listener.getClass().getName()
                        + " failed on " + event.type() + " — ignored", e);
            }
        }
    }

    @Override
    public void register(EventListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void unregister(EventListener listener) {
        listeners.remove(listener);
    }
}
