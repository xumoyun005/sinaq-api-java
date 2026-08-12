package io.sinaq.api.internal.extraction;

import io.sinaq.api.extraction.ExtractionProvider;
import io.sinaq.api.internal.json.JsonPathLite;

/** INTERNAL — wraps {@link JsonPathLite} for the extraction SPI. */
public final class BuiltInExtractionProvider implements ExtractionProvider {

    public static final BuiltInExtractionProvider INSTANCE = new BuiltInExtractionProvider();

    private BuiltInExtractionProvider() {}

    @Override
    public Object extract(Object jsonTree, String path) {
        return JsonPathLite.extract(jsonTree, path);
    }
}
