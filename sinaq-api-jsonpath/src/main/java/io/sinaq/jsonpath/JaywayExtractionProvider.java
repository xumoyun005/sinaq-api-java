package io.sinaq.jsonpath;

import com.jayway.jsonpath.JsonPath;
import io.sinaq.api.client.ApiClient;
import io.sinaq.api.extraction.ExtractionProvider;
import io.sinaq.api.plugin.SinaqPlugin;

/**
 * Full Jayway JSONPath {@link ExtractionProvider} (V2).
 */
public final class JaywayExtractionProvider implements ExtractionProvider {

    @Override
    public Object extract(Object jsonTree, String path) {
        String normalized = path.startsWith("$") ? path : "$." + path;
        return JsonPath.read(jsonTree, normalized);
    }

    /** Auto-registers Jayway extraction when used as a plugin. */
    public static final class Plugin implements SinaqPlugin {
        @Override public String id() { return "jsonpath"; }
        @Override public void onClientBuild(ApiClient.Builder builder) {
            builder.extractionProvider(new JaywayExtractionProvider());
        }
    }
}
