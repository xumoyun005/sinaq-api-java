package io.sinaq.api.assertion;

import io.sinaq.api.context.RequestContext;
import io.sinaq.api.http.HttpBody;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpMethod;
import io.sinaq.api.http.HttpRequest;
import io.sinaq.api.http.HttpTimeout;
import io.sinaq.api.masking.Masker;
import io.sinaq.api.support.StubHttpResponse;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AssertionFailureFormatterTest {

    @Test
    void formatsWithBodyAndCurl() {
        HttpRequest req = req(HttpMethod.POST, Optional.of(HttpBody.ofJson("{\"a\":1}")));
        var response = StubHttpResponse.json(500, "{\"error\":true}");
        String msg = AssertionFailureFormatter.format(
                "status == 200", 200, 500, req, response, Masker.defaults());
        assertThat(msg)
                .contains("Assertion failed: status == 200")
                .contains("expected=200")
                .contains("actual  =500")
                .contains("request-body:")
                .contains("response-body:")
                .contains("curl:");
    }

    @Test
    void formatsWithoutRequestBody() {
        HttpRequest req = req(HttpMethod.GET, Optional.empty());
        var response = StubHttpResponse.json(404, "{\"missing\":true}");
        String msg = AssertionFailureFormatter.format(
                "status == 200", 200, 404, req, response, Masker.defaults());
        assertThat(msg).contains("request: GET").doesNotContain("request-body:");
    }

    @Test
    void valueDiffEqualStringsAndTruncate() {
        assertThat(ValueDiff.format("x", "x")).contains("values equal as strings");
        assertThat(ValueDiff.stringify(null)).isEqualTo("null");
        assertThat(ValueDiff.truncate(null)).isEqualTo("null");
        String longText = "a".repeat(5000);
        assertThat(ValueDiff.truncate(longText)).contains("…(+");
        assertThat(ValueDiff.preview(longText, 10)).startsWith("aaaaaaaaaa…");
        assertThat(ValueDiff.preview(null, 10)).isEmpty();
        assertThat(ValueDiff.preview("short", 100)).isEqualTo("short");
    }

    @Test
    void curlRendererEmptyHeadersAndBodyPreview() {
        HttpRequest empty = req(HttpMethod.GET, Optional.of(HttpBody.empty()));
        assertThat(CurlRenderer.headerSummary(HttpHeaders.empty(), Masker.defaults())).isEqualTo("(none)");
        assertThat(CurlRenderer.bodyPreview(null, Masker.defaults(), 10)).isEqualTo("(empty)");
        assertThat(CurlRenderer.bodyPreview(HttpBody.empty(), Masker.defaults(), 10)).isEqualTo("(empty)");
        String curl = CurlRenderer.render(empty, Masker.defaults());
        assertThat(curl).contains("curl -sS -X GET").doesNotContain("--data-binary");
        assertThat(CurlRenderer.render(
                req(HttpMethod.POST, Optional.of(HttpBody.ofText("it's"))), Masker.defaults()))
                .contains("--data-binary");
    }

    private static HttpRequest req(HttpMethod method, Optional<HttpBody> body) {
        return new HttpRequest() {
            @Override public HttpMethod method() { return method; }
            @Override public URI uri() { return URI.create("https://api.example.com/x"); }
            @Override public HttpHeaders headers() { return HttpHeaders.empty(); }
            @Override public List<io.sinaq.api.http.HttpCookie> cookies() { return List.of(); }
            @Override public Optional<HttpBody> body() { return body; }
            @Override public HttpTimeout timeout() { return HttpTimeout.defaults(); }
            @Override public RequestContext context() { return RequestContext.create(); }
        };
    }
}
