package io.sinaq.junit5;

import io.sinaq.api.SinaqRuntime;
import io.sinaq.api.context.SinaqTestContextHolder;
import io.sinaq.api.context.TestContext;
import io.sinaq.api.events.EventType;
import io.sinaq.api.events.ReportEvent;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Map;

/**
 * JUnit 5 lifecycle adapter (spec §24).
 *
 * <p>Register via {@code @ExtendWith(SinaqExtension.class)} (or the
 * {@code junit.jupiter.extensions.autodetection.enabled} mechanism). Publishes
 * TEST_STARTED/TEST_FINISHED and binds the per-thread {@link TestContext}.</p>
 */
public final class SinaqExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        TestContext test = TestContext.create(context.getDisplayName(),
                SinaqRuntime.executionContext());
        SinaqTestContextHolder.set(test);
        SinaqRuntime.publisher().publish(
                ReportEvent.builder(EventType.TEST_STARTED, test.execution().executionId())
                        .testId(test.testId())
                        .payload(Map.of("test", test.testName()))
                        .build());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            TestContext test = SinaqTestContextHolder.get();
            if (test == null) {
                return;
            }
            String status = context.getExecutionException().isPresent() ? "failed" : "passed";
            SinaqRuntime.publisher().publish(
                    ReportEvent.builder(EventType.TEST_FINISHED, test.execution().executionId())
                            .testId(test.testId())
                            .payload(Map.of("test", test.testName(), "status", status))
                            .build());
        } finally {
            SinaqTestContextHolder.clear();
        }
    }
}
