package io.sinaq.api.assertion;

import io.sinaq.api.client.ApiClient;
import io.sinaq.api.client.Sinaq;
import io.sinaq.api.exception.SinaqAssertionException;
import io.sinaq.api.support.StubHttpEngine;
import io.sinaq.api.support.StubHttpResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SoftAssertionsTest {

    @Test
    void collectsFailuresAndAssertAllThrows() {
        StubHttpEngine engine = new StubHttpEngine();
        engine.respondJson(200, "{\"id\":1,\"items\":[1]}");
        ApiClient api = Sinaq.emptyClient().baseUrl("https://api.example.com").engine(engine).build();
        var r = api.get("/x").execute();

        assertThatThrownBy(() -> r.soft()
                .expectStatus(404)
                .expect("id", 99)
                .expectMatches("nope")
                .expectArraySize("$.items", 9)
                .assertAll())
                .isInstanceOf(SinaqAssertionException.class)
                .hasMessageContaining("soft assertion");
    }
}
