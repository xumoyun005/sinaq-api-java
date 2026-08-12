package io.sinaq.api.context;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ExecutionAndTestContextTest {

    @Test
    void executionContextCreation() {
        ExecutionContext e = ExecutionContext.create("int");
        assertThat(e.executionId()).isNotBlank();
        assertThat(e.environment()).isEqualTo("int");
        assertThat(e.startedAt()).isNotNull();
    }

    @Test
    void testContextCopiesMetadataDefensively() {
        ExecutionContext e = ExecutionContext.create("int");
        Map<String, Object> meta = new HashMap<>();
        meta.put("suite", "smoke");
        TestContext t = new TestContext("t1", "loginTest", e, meta);
        meta.put("suite", "changed");
        assertThat(t.metadata()).containsEntry("suite", "smoke");
        assertThatThrownBy(() -> t.metadata().put("x", 1))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
