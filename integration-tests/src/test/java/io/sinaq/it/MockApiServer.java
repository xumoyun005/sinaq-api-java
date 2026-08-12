package io.sinaq.it;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Local mock API used by all integration tests (spec §31). */
public final class MockApiServer implements AutoCloseable {

    private final HttpServer server;
    private final AtomicInteger flakyCounter = new AtomicInteger();

    public MockApiServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newFixedThreadPool(32));
        server.createContext("/echo", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respond(ex, 200, "{\"method\":\"" + ex.getRequestMethod()
                    + "\",\"query\":\"" + ex.getRequestURI().getRawQuery()
                    + "\",\"auth\":\"" + ex.getRequestHeaders().getFirst("Authorization")
                    + "\",\"body\":" + (body.isEmpty() ? "null" : body) + "}");
        });
        server.createContext("/users/", ex -> respond(ex, 200,
                "{\"id\":\"" + ex.getRequestURI().getPath().substring("/users/".length()) + "\"}"));
        server.createContext("/cards", ex -> respond(ex, 200,
                "{\"cards\":[{\"uuid\":\"u-1\"},{\"uuid\":\"u-2\"}]}"));
        server.createContext("/loan", ex -> respond(ex, 200,
                "{\"success\":true,\"loanId\":12345,\"accessToken\":\"tok-secret\"}"));
        server.createContext("/flaky", ex -> {
            int n = flakyCounter.incrementAndGet();
            respond(ex, n % 3 == 0 ? 200 : 503, "{\"attempt\":" + n + "}");
        });
        server.createContext("/slow", ex -> {
            try { Thread.sleep(1200); } catch (InterruptedException ignored) { }
            respond(ex, 200, "{\"slow\":true}");
        });
        server.createContext("/parallel", ex -> respond(ex, 200,
                "{\"marker\":\"" + ex.getRequestURI().getQuery().split("=")[1] + "\"}"));
        server.createContext("/poll", ex -> {
            int n = flakyCounter.incrementAndGet();
            respond(ex, n >= 3 ? 200 : 503, "{\"ready\":" + (n >= 3) + ",\"n\":" + n + "}");
        });
        server.createContext("/graphql", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            boolean hasQuery = body.contains("hello");
            respond(ex, 200, "{\"data\":{\"hello\":\"world\",\"echo\":" + hasQuery + "}}");
        });
        server.createContext("/upload", ex -> {
            String ct = ex.getRequestHeaders().getFirst("Content-Type");
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respond(ex, 200, "{\"multipart\":" + (ct != null && ct.contains("multipart"))
                    + ",\"hasField\":" + body.contains("name=") + "}");
        });
        server.createContext("/health", ex -> respond(ex, 200, "{\"status\":\"UP\"}"));
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void respond(HttpExchange ex, int status, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
