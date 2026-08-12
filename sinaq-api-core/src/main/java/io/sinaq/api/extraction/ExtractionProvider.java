package io.sinaq.api.extraction;

/**
 * JSON extraction SPI (V2 / ADR-004). Core ships a built-in subset provider;
 * plug in {@code sinaq-api-jsonpath} for full Jayway JSONPath support.
 */
public interface ExtractionProvider {

    /**
     * Extracts a value from a parsed JSON tree ({@code Map}/{@code List}/primitives).
     *
     * @param jsonTree parsed root (never null)
     * @param path     JSONPath expression
     * @return extracted value or {@code null} when absent
     */
    Object extract(Object jsonTree, String path);

    /** The dependency-free default (subset JSONPath). */
    static ExtractionProvider builtIn() {
        return io.sinaq.api.internal.extraction.BuiltInExtractionProvider.INSTANCE;
    }
}
