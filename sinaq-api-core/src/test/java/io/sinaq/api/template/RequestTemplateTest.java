package io.sinaq.api.template;

import io.sinaq.api.client.Sinaq;
import io.sinaq.api.http.HttpMethod;
import io.sinaq.api.support.StubHttpEngine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RequestTemplateTest {

    @Test
    void appliesTemplateToRequest() {
        StubHttpEngine engine = new StubHttpEngine();
        engine.respond(req -> {
            assertThat(req.headers().first("X-Tpl")).contains("yes");
            assertThat(req.uri().getQuery()).contains("v=2");
            return io.sinaq.api.support.StubHttpResponse.json(200, "{}");
        });
        var api = Sinaq.emptyClient()
                .baseUrl("https://api.example.com")
                .engine(engine)
                .registerTemplate(RequestTemplate.builder("loan", HttpMethod.POST, "/loan")
                        .header("X-Tpl", "yes")
                        .queryParam("v", "2")
                        .body(java.util.Map.of("amount", 100))
                        .build())
                .build();
        api.fromTemplate("loan").execute();
    }
}
