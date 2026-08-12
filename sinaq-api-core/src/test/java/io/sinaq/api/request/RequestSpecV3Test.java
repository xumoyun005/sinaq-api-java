package io.sinaq.api.request;

import io.sinaq.api.client.Sinaq;
import io.sinaq.api.http.HttpEngine;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.ImmutableHttpResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class RequestSpecV3Test {

    @Test
    void multipartSetsContentType() {
        AtomicReference<String> contentType = new AtomicReference<>();
        HttpEngine stub = new HttpEngine() {
            @Override
            public io.sinaq.api.http.HttpResponse execute(io.sinaq.api.http.HttpRequest request) {
                contentType.set(request.headers().first("Content-Type").orElse(null));
                return new ImmutableHttpResponse(200, HttpHeaders.empty(), new byte[0], Duration.ZERO);
            }
            @Override
            public String name() { return "stub"; }
        };
        Sinaq.emptyClient().baseUrl("http://localhost").engine(stub).build()
                .post("/up")
                .multiPart("name", "demo")
                .multiPartFile("file", "x".getBytes(), "f.txt")
                .execute();
        assertThat(contentType.get()).contains("multipart/form-data");
    }

    @Test
    void graphqlSetsJsonBody() {
        AtomicReference<String> body = new AtomicReference<>();
        HttpEngine stub = new HttpEngine() {
            @Override
            public io.sinaq.api.http.HttpResponse execute(io.sinaq.api.http.HttpRequest request) {
                body.set(request.body().map(b -> b.asString()).orElse(""));
                return new ImmutableHttpResponse(200, HttpHeaders.empty(), new byte[0], Duration.ZERO);
            }
            @Override
            public String name() { return "stub"; }
        };
        Sinaq.emptyClient().baseUrl("http://localhost").engine(stub).build()
                .post("/g")
                .graphql("{ hello }", java.util.Map.of("id", 1))
                .execute();
        assertThat(body.get()).contains("query");
        assertThat(body.get()).contains("variables");
    }
}
