package io.sinaq.api.messaging;

import java.util.Objects;
import java.util.Optional;

/** Expected message on a topic/queue (V4). */
public record MessageExpectation(
        Optional<String> bodyContains,
        Optional<String> headerEquals) {

    public MessageExpectation {
        bodyContains = bodyContains == null ? Optional.empty() : bodyContains;
        headerEquals = headerEquals == null ? Optional.empty() : headerEquals;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String bodyContains;
        private String headerEquals;

        public Builder bodyContains(String v) { bodyContains = v; return this; }
        public Builder headerEquals(String v) { headerEquals = v; return this; }

        public MessageExpectation build() {
            return new MessageExpectation(
                    Optional.ofNullable(bodyContains),
                    Optional.ofNullable(headerEquals));
        }
    }

    void verify(MessageRecord message) {
        Objects.requireNonNull(message, "message");
        bodyContains.ifPresent(part -> {
            if (!message.body().contains(part)) {
                throw new io.sinaq.api.exception.SinaqAssertionException(
                        "Message body missing '" + part + "': " + message.body());
            }
        });
        headerEquals.ifPresent(expected -> {
            if (!expected.equals(message.header())) {
                throw new io.sinaq.api.exception.SinaqAssertionException(
                        "Message header mismatch — expected: " + expected + ", actual: " + message.header());
            }
        });
    }
}
