package io.sinaq.api.internal.log;

import java.io.PrintStream;
import java.util.function.BiConsumer;

/**
 * INTERNAL — minimal logging abstraction of the core (spec §28).
 *
 * <p>The core stays dependency-free: by default messages go to {@code System.err}.
 * A logging adapter (e.g. SLF4J bridge) may replace the sink via {@link #setSink}.
 * Never log sensitive values through this class — mask first.</p>
 */
public final class SinaqLog {

    private static volatile BiConsumer<String, Throwable> sink = SinaqLog::defaultSink;

    private SinaqLog() {}

    public static void warn(String message, Throwable t) {
        sink.accept(message, t);
    }

    /** Replaces the log sink. Pass null to restore the default. */
    public static void setSink(BiConsumer<String, Throwable> newSink) {
        sink = newSink != null ? newSink : SinaqLog::defaultSink;
    }

    private static void defaultSink(String message, Throwable t) {
        PrintStream err = System.err;
        err.println("[sinaq] WARN " + message);
        if (t != null) {
            t.printStackTrace(err);
        }
    }
}
