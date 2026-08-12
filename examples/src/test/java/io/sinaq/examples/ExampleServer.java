package io.sinaq.examples;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/** Tiny echo server so examples run offline in CI. */
final class ExampleServer implements AutoCloseable {

    private final HttpServer server;

    ExampleServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/get", ex -> respond(ex, 200,
                "{\"page\":\"" + queryParam(ex, "page") + "\",\"origin\":\"127.0.0.1\"}"));
        server.createContext("/post", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respond(ex, 200, "{\"auth\":\"" + ex.getRequestHeaders().getFirst("Authorization")
                    + "\",\"body\":" + (body.isEmpty() ? "null" : body) + "}");
        });
        server.createContext("/cards", ex -> respond(ex, 200,
                "{\"cards\":[{\"uuid\":\"u-1\"},{\"uuid\":\"u-2\"}]}"));
        server.createContext("/poll", ex -> {
            int n = pollCounter.incrementAndGet();
            respond(ex, n >= 2 ? 200 : 503, "{\"ready\":" + (n >= 2) + "}");
        });
        server.createContext("/token", ex -> respond(ex, 200,
                "{\"access_token\":\"demo-oauth-token\",\"expires_in\":3600}"));
        server.start();
    }

    private final java.util.concurrent.atomic.AtomicInteger pollCounter =
            new java.util.concurrent.atomic.AtomicInteger();

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static String queryParam(HttpExchange ex, String name) {
        String query = ex.getRequestURI().getRawQuery();
        if (query == null) return "";
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) return kv[1];
        }
        return "";
    }

    private static void respond(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
