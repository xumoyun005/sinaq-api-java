package io.sinaq.api.polling;

import io.sinaq.api.SinaqRuntime;
import io.sinaq.api.events.EventType;
import io.sinaq.api.events.ReportEvent;
import io.sinaq.api.exception.SinaqAssertionException;
import io.sinaq.api.exception.SinaqTimeoutException;
import io.sinaq.api.request.RequestSpec;
import io.sinaq.api.response.ApiResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Polls a request until a condition is met (V2).
 */
public final class PollSpec {

    private final RequestSpec template;
    private Duration interval = Duration.ofMillis(500);
    private Duration timeout = Duration.ofSeconds(30);
    private Predicate<ApiResponse> until = r -> r.status() >= 200 && r.status() < 300;

    public PollSpec(RequestSpec spec) {
        this.template = Objects.requireNonNull(spec, "spec");
    }

    public PollSpec interval(Duration interval) {
        this.interval = Objects.requireNonNull(interval);
        return this;
    }

    public PollSpec timeout(Duration timeout) {
        this.timeout = Objects.requireNonNull(timeout);
        return this;
    }

    public PollSpec untilStatus(int status) {
        this.until = r -> r.status() == status;
        return this;
    }

    public PollSpec until(Predicate<ApiResponse> condition) {
        this.until = Objects.requireNonNull(condition);
        return this;
    }

    public ApiResponse execute() {
        publish(EventType.POLL_STARTED, Map.of("timeoutMs", timeout.toMillis()));
        Instant deadline = Instant.now().plus(timeout);
        int attempt = 0;
        ApiResponse last = null;
        while (Instant.now().isBefore(deadline)) {
            attempt++;
            publish(EventType.POLL_ATTEMPT, Map.of("attempt", attempt));
            last = template.duplicate().execute();
            if (until.test(last)) {
                publish(EventType.POLL_SUCCEEDED, Map.of("attempt", attempt, "status", last.status()));
                return last;
            }
            sleep(interval);
        }
        publish(EventType.POLL_FAILED, Map.of("attempts", attempt));
        throw new SinaqTimeoutException(
                "Poll timed out after " + timeout + " (" + attempt + " attempts)",
                null, null);
    }

    private void sleep(Duration d) {
        if (d.isZero()) return;
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SinaqAssertionException("Interrupted during poll interval", e);
        }
    }

    private void publish(EventType type, Map<String, Object> payload) {
        SinaqRuntime.publisher().publish(ReportEvent.builder(type,
                        SinaqRuntime.executionContext().executionId())
                .payload(payload)
                .build());
    }
}
