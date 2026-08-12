package io.sinaq.api.messaging;

import io.sinaq.api.exception.SinaqAssertionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MessageExpectationTest {

    @Test
    void verifiesBodyAndHeader() {
        MessageExpectation.builder()
                .bodyContains("ok")
                .headerEquals("h1")
                .build()
                .verify(new MessageRecord("t", "ok-body", "h1", java.time.Instant.now()));
    }

    @Test
    void failsOnBodyMismatch() {
        assertThatThrownBy(() -> MessageExpectation.builder().bodyContains("missing").build()
                .verify(new MessageRecord("t", "other", "", java.time.Instant.now())))
                .isInstanceOf(SinaqAssertionException.class);
    }
}
