package io.sinaq.api.context;

/**
 * Thread-local holder linking the currently running test to the requests it
 * makes — the framework's single justified ThreadLocal (spec §18): TestNG/JUnit
 * callbacks offer no other channel to per-thread test identity.
 *
 * <p>Adapters set the context before a test and MUST clear it in a finally
 * block after the test. Core reads it to stamp {@code testId} onto events.</p>
 */
public final class SinaqTestContextHolder {

    private static final ThreadLocal<TestContext> CURRENT = new ThreadLocal<>();

    private SinaqTestContextHolder() {}

    public static void set(TestContext context) {
        CURRENT.set(context);
    }

    public static TestContext get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
