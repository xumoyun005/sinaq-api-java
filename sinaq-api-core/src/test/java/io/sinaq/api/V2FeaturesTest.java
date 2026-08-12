package io.sinaq.api;

import io.sinaq.api.auth.BearerTokenAuth;
import io.sinaq.api.auth.TokenCache;
import io.sinaq.api.client.Sinaq;
import io.sinaq.api.config.EnvironmentProfile;
import io.sinaq.api.events.EventType;
import io.sinaq.api.parallel.ParallelRequests;
import io.sinaq.api.plugin.PluginRegistry;
import io.sinaq.api.plugin.SinaqPlugin;
import io.sinaq.api.support.StubHttpEngine;
import io.sinaq.api.support.StubHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class V2FeaturesTest {

    private final StubHttpEngine engine = new StubHttpEngine();

    @AfterEach
    void tearDown() {
        SinaqRuntime.reset();
        PluginRegistry.global().clear();
    }

    @Test
    void parallelRequestsExecuteIndependently() {
        engine.respond(req -> StubHttpResponse.json(200,
                "{\"m\":\"" + req.uri().getQuery().split("=")[1] + "\"}"));
        var api = Sinaq.emptyClient().baseUrl("https://api.example.com").engine(engine).build();
        List<?> results = ParallelRequests.executeAll(List.of(
                () -> api.get("/p").queryParam("m", "a"),
                () -> api.get("/p").queryParam("m", "b")), 2);
        assertThat(results).hasSize(2);
    }

    @Test
    void pollingUntilCondition() {
        AtomicInteger n = new AtomicInteger();
        engine.respond(req -> {
            int v = n.incrementAndGet();
            return StubHttpResponse.json(v >= 2 ? 200 : 503, "{\"ok\":" + (v >= 2) + "}");
        });
        var api = Sinaq.emptyClient().baseUrl("https://api.example.com").engine(engine).build();
        Boolean ok = api.get("/poll").poll().interval(Duration.ZERO).timeout(Duration.ofSeconds(2))
                .until(r -> r.status() == 200)
                .execute().extract("$.ok");
        assertThat(ok).isTrue();
    }

    @Test
    void environmentProfileAndPlugins() {
        PluginRegistry.global().register(new SinaqPlugin() {
            @Override public String id() { return "test-plugin"; }
        });
        assertThat(EnvironmentProfile.from("int")).isEqualTo(EnvironmentProfile.INT);
        assertThat(PluginRegistry.global().plugins()).hasSize(1);
    }

    @Test
    void tokenCacheAndBearerTokenAuth() {
        TokenCache cache = new TokenCache();
        cache.put("tok-1", null);
        assertThat(cache.get()).contains("tok-1");
        cache.invalidate();
        assertThat(cache.get()).isEmpty();

        engine.respond(req -> {
            assertThat(req.headers().first("Authorization")).contains("Bearer live");
            return StubHttpResponse.json(200, "{}");
        });
        var api = Sinaq.emptyClient().baseUrl("https://api.example.com").engine(engine).build();
        api.get("/x").auth(new BearerTokenAuth(() -> "live")).execute();
    }

    @Test
    void extractListAndDuplicate() {
        engine.respondJson(200, "{\"items\":[{\"id\":1},{\"id\":2}]}");
        var api = Sinaq.emptyClient().baseUrl("https://api.example.com").engine(engine).build();
        assertThat(api.get("/x").execute().extractList("$.items[*].id"))
                .containsExactly(1L, 2L);
        var spec = api.get("/y");
        assertThat(spec.duplicate()).isNotSameAs(spec);
    }
}
