package io.sinaq.api.events;

/**
 * Subscriber of framework events (report writers, loggers, metrics...).
 *
 * <p>Exceptions thrown by a listener are caught and logged by the publisher —
 * a broken reporter must never fail a test.</p>
 */
@FunctionalInterface
public interface EventListener {

    void onEvent(ReportEvent event);
}
