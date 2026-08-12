package io.sinaq.api.auth;

import io.sinaq.api.client.ApiClient;
import io.sinaq.api.client.Sinaq;
import io.sinaq.api.support.StubHttpEngine;
import io.sinaq.api.support.StubHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class AuthProvidersTest {

    private final StubHttpEngine engine = new StubHttpEngine();
    private ApiClient api;

    @BeforeEach
    void setUp() {
        api = Sinaq.emptyClient().baseUrl("https://api.example.com").engine(engine).build();
        engine.respond(req -> StubHttpResponse.json(200, "{}"));
    }

    @Test
    void bearerAuth() {
        api.get("/x").bearer("tok-abc").execute();
        assertThat(engine.lastRequest().headers().first("Authorization")).contains("Bearer tok-abc");
    }

    @Test
    void basicAuth() {
        api.get("/x").basicAuth("user", "pass").execute();
        String expected = "Basic " + Base64.getEncoder()
                .encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
        assertThat(engine.lastRequest().headers().first("Authorization")).contains(expected);
    }

    @Test
    void apiKeyAuthDefaultAndCustomHeader() {
        api.get("/x").apiKey("k1").execute();
        assertThat(engine.lastRequest().headers().first("X-Api-Key")).contains("k1");

        api.get("/y").auth(new ApiKeyAuth("X-Key", "k2")).execute();
        assertThat(engine.lastRequest().headers().first("X-Key")).contains("k2");
    }

    @Test
    void customAuth() {
        api.get("/x").auth(new CustomAuth(spec -> spec.header("X-Custom-Auth", "secret"))).execute();
        assertThat(engine.lastRequest().headers().first("X-Custom-Auth")).contains("secret");
    }
}
