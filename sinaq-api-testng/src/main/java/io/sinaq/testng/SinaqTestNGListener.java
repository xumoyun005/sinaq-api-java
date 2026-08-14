package io.sinaq.testng;

import io.sinaq.api.SinaqRuntime;
import io.sinaq.api.context.SinaqTestContextHolder;
import io.sinaq.api.context.TestContext;
import io.sinaq.api.events.EventType;
import io.sinaq.api.events.ReportEvent;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Map;

/**
 * TestNG lifecycle adapter (spec §23).
 *
 * <p>Publishes TEST_STARTED/TEST_FINISHED events and binds a {@link TestContext}
 * to the executing thread so request events carry {@code testId}. Register via
 * {@code @Listeners(SinaqTestNGListener.class)} or testng.xml. Sinaq assertion
 * failures ({@code SinaqAssertionException}) surface as normal TestNG failures —
 * no mapping is required because they are unchecked exceptions.</p>
 */
public final class SinaqTestNGListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        TestContext context = TestContext.create(qualifiedName(result),
                SinaqRuntime.executionContext());
        SinaqTestContextHolder.set(context);
        publish(EventType.TEST_STARTED, context, Map.of("test", context.testName()));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        finish(result, "PASSED", null);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Throwable t = result.getThrowable();
        finish(result, "FAILED", t == null ? "" : String.valueOf(t.getMessage()));
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        finish(result, "SKIPPED", null);
    }

    private void finish(ITestResult result, String status, String message) {
        try {
            TestContext context = SinaqTestContextHolder.get();
            if (context == null) {
                context = TestContext.create(qualifiedName(result), SinaqRuntime.executionContext());
            }
            Map<String, Object> payload = message == null
                    ? Map.of("test", context.testName(), "status", status)
                    : Map.of("test", context.testName(), "status", status, "message", message);
            publish(EventType.TEST_FINISHED, context, payload);
        } finally {
            SinaqTestContextHolder.clear();
        }
    }

    private static void publish(EventType type, TestContext context, Map<String, Object> payload) {
        SinaqRuntime.publisher().publish(
                ReportEvent.builder(type, context.execution().executionId())
                        .testId(context.testId())
                        .payload(payload)
                        .build());
    }

    private static String qualifiedName(ITestResult result) {
        return result.getTestClass().getName() + "." + result.getMethod().getMethodName();
    }
}
