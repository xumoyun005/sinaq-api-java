package io.sinaq.api.assertion;

import io.sinaq.api.context.RequestContext;
import io.sinaq.api.http.HttpBody;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpMethod;
import io.sinaq.api.http.HttpRequest;
import io.sinaq.api.http.HttpTimeout;
import io.sinaq.api.masking.Masker;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CurlRendererTest {

    @Test
    void masksAuthorizationAndIncludesBody() {
        HttpRequest req = new SimpleRequest(
                HttpMethod.POST,
                URI.create("https://api.example.com/loan"),
                HttpHeaders.builder()
                        .add("Authorization", "Bearer secret-token-value")
                        .add("Content-Type", "application/json")
                        .build(),
                Optional.of(HttpBody.ofJson("{\"amount\":1}")));

        String curl = CurlRenderer.render(req, Masker.defaults());
        assertThat(curl).contains("curl -sS -X POST");
        assertThat(curl).contains("https://api.example.com/loan");
        assertThat(curl).contains("Authorization: Bearer ****");
        assertThat(curl).doesNotContain("secret-token-value");
        assertThat(curl).contains("--data-binary");
    }

    @Test
    void valueDiffShowsExpectedAndActual() {
        String diff = ValueDiff.format(404, 200);
        assertThat(diff).contains("expected=404");
        assertThat(diff).contains("actual  =200");
    }

    private record SimpleRequest(
            HttpMethod method,
            URI uri,
            HttpHeaders headers,
            Optional<HttpBody> body
    ) implements HttpRequest {
        @Override public List<io.sinaq.api.http.HttpCookie> cookies() { return List.of(); }
        @Override public HttpTimeout timeout() { return HttpTimeout.defaults(); }
        @Override public RequestContext context() {
            return RequestContext.create();
        }
    }
}
