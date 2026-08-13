package io.sinaq.api.assertion;

import io.sinaq.api.http.HttpRequest;
import io.sinaq.api.http.HttpResponse;
import io.sinaq.api.masking.Masker;

/**
 * Builds multi-line assertion failure messages with diff, masked RR, and curl.
 */
public final class AssertionFailureFormatter {

    private static final int BODY_PREVIEW = 2048;

    private AssertionFailureFormatter() {}

    public static String format(String description,
                                Object expected,
                                Object actual,
                                HttpRequest request,
                                HttpResponse response,
                                Masker masker) {
        StringBuilder sb = new StringBuilder();
        sb.append("Assertion failed: ").append(description).append('\n');
        sb.append(ValueDiff.format(expected, actual)).append('\n');
        sb.append("request: ").append(request.method().name()).append(' ').append(request.uri()).append('\n');
        sb.append("request-headers: ").append(CurlRenderer.headerSummary(request.headers(), masker)).append('\n');
        if (request.body().isPresent() && !request.body().get().isEmpty()) {
            sb.append("request-body: ")
                    .append(CurlRenderer.bodyPreview(request.body().get(), masker, BODY_PREVIEW))
                    .append('\n');
        }
        sb.append("response: ").append(response.statusCode()).append('\n');
        String respBody = new String(response.rawBody(), java.nio.charset.StandardCharsets.UTF_8);
        sb.append("response-body: ")
                .append(ValueDiff.preview(masker.maskText(respBody), BODY_PREVIEW))
                .append('\n');
        sb.append("curl: ").append(CurlRenderer.render(request, masker));
        return sb.toString();
    }
}
