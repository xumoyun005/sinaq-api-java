package io.sinaq.api.context;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Identifies one test (method/scenario) inside an execution.
 * Created by test-framework adapters (TestNG/JUnit 5). Immutable; thread-safe.
 */
public record TestContext(String testId,
                          String testName,
                          ExecutionContext execution,
                          Map<String, Object> metadata) {

    public TestContext {
        Objects.requireNonNull(testId, "testId");
        Objects.requireNonNull(testName, "testName");
        Objects.requireNonNull(execution, "execution");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static TestContext create(String testName, ExecutionContext execution) {
        return new TestContext(UUID.randomUUID().toString(), testName, execution, Map.of());
    }
}
