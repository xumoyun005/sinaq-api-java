package io.sinaq.api.assertion;

/**
 * Compact expected-vs-actual formatting for assertion failures.
 * Truncates long values; highlights inequality for scalars.
 */
public final class ValueDiff {

    private static final int MAX = 4096;

    private ValueDiff() {}

    public static String format(Object expected, Object actual) {
        String e = truncate(stringify(expected));
        String a = truncate(stringify(actual));
        if (e.equals(a)) {
            return "expected=" + e + "\nactual  =" + a + " (values equal as strings; type or path mismatch?)";
        }
        return "expected=" + e + "\nactual  =" + a;
    }

    public static String stringify(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            return s;
        }
        return String.valueOf(value);
    }

    public static String truncate(String text) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= MAX) {
            return text;
        }
        return text.substring(0, MAX) + "…(+ " + (text.length() - MAX) + " chars)";
    }

    /** Shorter preview for response bodies (~2KB). */
    public static String preview(String text, int max) {
        if (text == null) {
            return "";
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "…(+ " + (text.length() - max) + " chars)";
    }
}
