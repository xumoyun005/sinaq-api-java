package io.sinaq.api.assertion;

import io.sinaq.api.http.HttpBody;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpRequest;
import io.sinaq.api.masking.Masker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders a copy-pasteable curl for a resolved {@link HttpRequest}, with secrets masked.
 */
public final class CurlRenderer {

    private CurlRenderer() {}

    public static String render(HttpRequest request, Masker masker) {
        StringBuilder sb = new StringBuilder("curl -sS -X ").append(request.method().name());
        sb.append(" '").append(shellEscape(request.uri().toString())).append("'");

        Map<String, List<String>> headers = masker.maskHeaders(request.headers());
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            for (String value : e.getValue()) {
                sb.append(" -H '").append(shellEscape(e.getKey())).append(": ")
                        .append(shellEscape(value)).append("'");
            }
        }

        request.body().ifPresent(body -> {
            if (!body.isEmpty()) {
                String raw = body.asString();
                String masked = masker.maskText(raw);
                sb.append(" --data-binary '").append(shellEscape(masked)).append("'");
            }
        });
        return sb.toString();
    }

    public static String headerSummary(HttpHeaders headers, Masker masker) {
        Map<String, List<String>> masked = masker.maskHeaders(headers);
        if (masked.isEmpty()) {
            return "(none)";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : masked.entrySet()) {
            parts.add(e.getKey() + "=" + String.join(",", e.getValue()));
        }
        return String.join("; ", parts);
    }

    public static String bodyPreview(HttpBody body, Masker masker, int max) {
        if (body == null || body.isEmpty()) {
            return "(empty)";
        }
        return ValueDiff.preview(masker.maskText(body.asString()), max);
    }

    private static String shellEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("'", "'\\''");
    }
}
