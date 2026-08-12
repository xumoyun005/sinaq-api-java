package io.sinaq.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock stub server wrapper for contract tests (V4).
 */
public final class SinaqWireMock implements AutoCloseable {

    private final WireMockServer server;

    public SinaqWireMock() {
        this.server = new WireMockServer(com.github.tomakehurst.wiremock.core.WireMockConfiguration
                .wireMockConfig().dynamicPort());
        server.start();
        WireMock.configureFor("localhost", server.port());
    }

    public String baseUrl() {
        return "http://localhost:" + server.port();
    }

    public WireMockServer server() {
        return server;
    }

    public void stubGet(String path, int status, String jsonBody) {
        Objects.requireNonNull(path, "path");
        server.stubFor(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonBody == null ? "" : jsonBody)));
    }

    public void stubPost(String path, int status, String jsonBody) {
        server.stubFor(post(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonBody == null ? "" : jsonBody)));
    }

    public int requestCount(String path) {
        return server.findAll(getRequestedFor(urlEqualTo(path))).size()
                + server.findAll(postRequestedFor(urlEqualTo(path))).size();
    }

    @Override
    public void close() {
        server.stop();
    }
}
