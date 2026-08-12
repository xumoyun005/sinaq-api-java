package io.sinaq.api.client;

import io.sinaq.api.config.EnvironmentProfile;
import io.sinaq.api.config.RetryPolicy;
import io.sinaq.api.exception.SinaqSerializationException;
import io.sinaq.api.extraction.ExtractionProvider;
import io.sinaq.api.http.HttpMethod;
import io.sinaq.api.parallel.ParallelRequests;
import io.sinaq.api.plugin.SinaqPlugin;
import io.sinaq.api.template.RequestTemplate;
import io.sinaq.api.support.StubHttpEngine;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ApiClientV2Test {

    @Test
    void builderWiresV2Capabilities() {
        StubHttpEngine engine = new StubHttpEngine();
        engine.respondJson(200, "{\"a\":1}");
        ExtractionProvider custom = (tree, path) -> 42;
        ApiClient api = Sinaq.emptyClient()
                .baseUrl("https://api.example.com")
                .profile(EnvironmentProfile.TEST)
                .engine(engine)
                .extractionProvider(custom)
                .defaultRetry(RetryPolicy.builder().maxAttempts(2).onStatus(503).build())
                .registerTemplate(RequestTemplate.builder("t", HttpMethod.GET, "/t").build())
                .plugin(new SinaqPlugin() {
                    @Override public String id() { return "noop"; }
                })
                .build();
        assertThat(api.config().environment()).isEqualTo("test");
        Object extracted = api.get("/x").execute().extract("$.any");
        assertThat(extracted).isEqualTo(42);
        api.fromTemplate("t").execute();
        assertThat(ParallelRequests.executeAll(List.of(), 2)).isEmpty();
    }

    @Test
    void extractListRejectsNonList() {
        StubHttpEngine engine = new StubHttpEngine();
        engine.respondJson(200, "{\"a\":1}");
        var api = Sinaq.emptyClient().baseUrl("https://api.example.com").engine(engine).build();
        assertThatThrownBy(() -> api.get("/x").execute().extractList("$.a"))
                .isInstanceOf(SinaqSerializationException.class);
    }
}
