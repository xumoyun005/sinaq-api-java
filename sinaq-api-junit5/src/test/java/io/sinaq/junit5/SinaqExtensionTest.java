package io.sinaq.junit5;

import io.sinaq.api.SinaqRuntime;
import io.sinaq.api.context.SinaqTestContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(SinaqExtension.class)
class SinaqExtensionTest {

    @AfterEach
    void tearDown() {
        SinaqRuntime.reset();
    }

    @Test
    void bindsTestContextPerThread() {
        assertThat(SinaqTestContextHolder.get()).isNotNull();
        assertThat(SinaqTestContextHolder.get().testName()).contains("bindsTestContext");
        assertThat(SinaqTestContextHolder.get().testId()).isNotBlank();
    }
}
