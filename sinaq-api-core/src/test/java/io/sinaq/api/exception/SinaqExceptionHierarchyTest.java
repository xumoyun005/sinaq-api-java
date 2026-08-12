package io.sinaq.api.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SinaqExceptionHierarchyTest {

    @Test
    void allSinaqExceptionsExtendBase() {
        assertThat(new SinaqConfigurationException("x")).isInstanceOf(SinaqException.class);
        assertThat(new SinaqHttpException("x")).isInstanceOf(SinaqException.class);
        assertThat(new SinaqAssertionException("x")).isInstanceOf(SinaqException.class);
        assertThat(new SinaqSerializationException("x")).isInstanceOf(SinaqException.class);
    }

    @Test
    void transportHierarchy_engineAndTimeoutAreHttpFailures() {
        assertThat(new SinaqEngineException("x")).isInstanceOf(SinaqHttpException.class);
        assertThat(new SinaqTimeoutException("x")).isInstanceOf(SinaqEngineException.class);
        assertThat(new SinaqTimeoutException("x")).isInstanceOf(SinaqHttpException.class);
    }

    @Test
    void correlationIdAppearsInMessageWhenPresent() {
        SinaqException withId = new SinaqHttpException("boom", null, "corr-123");
        SinaqException withoutId = new SinaqHttpException("boom");
        assertThat(withId.correlationId()).contains("corr-123");
        assertThat(withId.getMessage()).isEqualTo("boom [correlationId=corr-123]");
        assertThat(withoutId.correlationId()).isEmpty();
        assertThat(withoutId.getMessage()).isEqualTo("boom");
    }

    @Test
    void causeIsPreserved() {
        RuntimeException cause = new RuntimeException("root");
        assertThat(new SinaqEngineException("wrap", cause).getCause()).isSameAs(cause);
    }
}
