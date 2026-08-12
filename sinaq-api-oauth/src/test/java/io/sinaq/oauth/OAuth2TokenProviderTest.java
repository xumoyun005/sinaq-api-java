package io.sinaq.oauth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class OAuth2TokenProviderTest {

    private HttpServer server;
    private final AtomicInteger tokenCalls = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        tokenCalls.set(0);
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/token", ex -> {
            tokenCalls.incrementAndGet();
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(body).contains("grant_type=client_credentials");
            assertThat(body).contains("client_id=demo-client");
            respond(ex, 200, "{\"access_token\":\"tok-abc\",\"expires_in\":3600}");
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void fetchesAndCachesAccessToken() {
        OAuth2TokenProvider.OAuth2Config config = OAuth2TokenProvider.OAuth2Config.builder(tokenUrl())
                .clientId("demo-client")
                .clientSecret("demo-secret")
                .scope("read")
                .build();
        OAuth2TokenProvider provider = new OAuth2TokenProvider(config);

        assertThat(provider.accessToken()).isEqualTo("tok-abc");
        assertThat(provider.accessToken()).isEqualTo("tok-abc");
        assertThat(tokenCalls.get()).isEqualTo(1);
    }

    @Test
    void invalidateForcesRefresh() {
        OAuth2TokenProvider.OAuth2Config config = OAuth2TokenProvider.OAuth2Config.builder(tokenUrl())
                .clientId("demo-client")
                .clientSecret("demo-secret")
                .build();
        OAuth2TokenProvider provider = new OAuth2TokenProvider(config);
        provider.accessToken();
        provider.invalidate();
        provider.accessToken();
        assertThat(tokenCalls.get()).isEqualTo(2);
    }

    @Test
    void passwordGrantFetchesToken() {
        server.createContext("/token-password", ex -> {
            tokenCalls.incrementAndGet();
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(body).contains("grant_type=password");
            assertThat(body).contains("username=alice");
            respond(ex, 200, "{\"access_token\":\"pwd-tok\",\"expires_in\":3600}");
        });
        OAuth2TokenProvider.OAuth2Config config = OAuth2TokenProvider.OAuth2Config.builder(
                        "http://127.0.0.1:" + server.getAddress().getPort() + "/token-password")
                .grantType("password")
                .clientId("demo-client")
                .clientSecret("demo-secret")
                .username("alice")
                .password("secret")
                .build();
        assertThat(new OAuth2TokenProvider(config).accessToken()).isEqualTo("pwd-tok");
    }

    private String tokenUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/token";
    }

    private static void respond(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
