package io.sinaq.api.auth;

import io.sinaq.api.client.Sinaq;
import io.sinaq.api.config.EnvironmentProfile;
import io.sinaq.api.exception.SinaqConfigurationException;
import io.sinaq.api.exception.SinaqTimeoutException;
import io.sinaq.api.support.StubHttpEngine;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class AuthAndProfileV2Test {

    @Test
    void refreshingTokenProviderCaches() {
        AtomicInteger calls = new AtomicInteger();
        RefreshingTokenProvider tokens = new RefreshingTokenProvider(() -> {
            calls.incrementAndGet();
            return new RefreshingTokenProvider.TokenResponse("tok-" + calls.get(),
                    Instant.now().plusSeconds(120));
        });
        assertThat(tokens.accessToken()).isEqualTo("tok-1");
        assertThat(tokens.accessToken()).isEqualTo("tok-1");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void jwtAuthAddsBearerHeader() {
        StubHttpEngine engine = new StubHttpEngine();
        engine.respond(req -> {
            assertThat(req.headers().first("Authorization")).contains("Bearer jwt-xyz");
            return io.sinaq.api.support.StubHttpResponse.json(200, "{}");
        });
        Sinaq.emptyClient().baseUrl("https://api.example.com").engine(engine).build()
                .get("/x").auth(new JwtAuth("jwt-xyz")).execute();
    }

    @Test
    void environmentProfileRejectsUnknown() {
        assertThatThrownBy(() -> EnvironmentProfile.from("unknown"))
                .isInstanceOf(SinaqConfigurationException.class);
        assertThat(EnvironmentProfile.from(null)).isEqualTo(EnvironmentProfile.DEV);
    }

    @Test
    void pollTimesOut() {
        StubHttpEngine engine = new StubHttpEngine();
        engine.respondJson(503, "{\"ready\":false}");
        var api = Sinaq.emptyClient().baseUrl("https://api.example.com").engine(engine).build();
        assertThatThrownBy(() -> api.get("/x").poll()
                .interval(Duration.ZERO)
                .timeout(Duration.ofMillis(50))
                .until(r -> r.status() == 200)
                .execute())
                .isInstanceOf(SinaqTimeoutException.class);
    }
}
