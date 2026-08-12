package io.sinaq.api;

import io.sinaq.api.context.ExecutionContext;
import io.sinaq.api.events.EventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SinaqRuntimeTest {

    @AfterEach
    void tearDown() {
        SinaqRuntime.reset();
    }

    @Test
    void publisherAndExecutionContextAreReplaceable() {
        EventPublisher custom = EventPublisher.createSync();
        ExecutionContext ctx = ExecutionContext.create("test-env");
        SinaqRuntime.setPublisher(custom);
        SinaqRuntime.setExecutionContext(ctx);
        assertThat(SinaqRuntime.publisher()).isSameAs(custom);
        assertThat(SinaqRuntime.executionContext()).isSameAs(ctx);
        SinaqRuntime.reset();
        assertThat(SinaqRuntime.executionContext().environment()).isEqualTo("default");
    }
}
