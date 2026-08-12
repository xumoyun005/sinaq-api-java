package io.sinaq.api.replay;

import io.sinaq.api.client.ApiClient;
import io.sinaq.api.exception.SinaqAssertionException;
import io.sinaq.api.http.HttpMethod;
import io.sinaq.api.recording.RecordedExchange;
import io.sinaq.api.request.RequestSpec;
import io.sinaq.api.response.ApiResponse;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Replays recorded exchanges against a live client (V4).
 */
public final class ReplayPlayer {

    private ReplayPlayer() {}

    public static ReplayResult replay(ApiClient client, List<RecordedExchange> exchanges) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(exchanges, "exchanges");
        int passed = 0;
        for (RecordedExchange ex : exchanges) {
            replayOne(client, ex);
            passed++;
        }
        return new ReplayResult(passed);
    }

    private static void replayOne(ApiClient client, RecordedExchange ex) {
        String path = URI.create(ex.url()).getRawPath();
        RequestSpec spec = switch (HttpMethod.valueOf(ex.method())) {
            case GET -> client.get(path);
            case POST -> client.post(path);
            case PUT -> client.put(path);
            case PATCH -> client.patch(path);
            case DELETE -> client.delete(path);
            case HEAD -> client.head(path);
            case OPTIONS -> client.options(path);
        };
        if (!ex.requestBody().isBlank() && !"null".equals(ex.requestBody())) {
            spec.json(ex.requestBody());
        }
        ApiResponse response = spec.execute();
        if (response.status() != ex.status()) {
            throw new SinaqAssertionException(
                    "Replay status mismatch for " + ex.method() + " " + path
                    + " — expected: " + ex.status() + ", actual: " + response.status());
        }
    }

    public record ReplayResult(int replayedCount) {}
}
