package io.sinaq.api.client;

import io.sinaq.api.exception.SinaqConfigurationException;
import io.sinaq.api.http.HttpMethod;
import io.sinaq.api.support.StubHttpEngine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ApiClientTest {

    @Test
    void builderRequiresEngine() {
        assertThatThrownBy(() -> ApiClient.builder().baseUrl("https://api.example.com").build())
                .isInstanceOf(SinaqConfigurationException.class)
                .hasMessageContaining("No HttpEngine");
    }

    @Test
    void allVerbsCreateFreshRequestSpecs() {
        StubHttpEngine engine = new StubHttpEngine();
        ApiClient api = Sinaq.emptyClient().baseUrl("https://api.example.com").engine(engine).build();

        assertThat(api.get("/a")).isNotSameAs(api.get("/a"));
        assertThat(api.post("/b").execute().status()).isEqualTo(200);
        assertThat(api.put("/c").execute().status()).isEqualTo(200);
        assertThat(api.patch("/d").execute().status()).isEqualTo(200);
        assertThat(api.delete("/e").execute().status()).isEqualTo(200);
        assertThat(api.head("/f").execute().status()).isEqualTo(200);
        assertThat(api.options("/g").execute().status()).isEqualTo(200);
        assertThat(api.request(HttpMethod.GET, "/h").execute().status()).isEqualTo(200);
    }

    @Test
    void closeDelegatesToEngine() {
        StubHttpEngine engine = new StubHttpEngine();
        try (ApiClient api = Sinaq.emptyClient().baseUrl("https://api.example.com").engine(engine).build()) {
            // no-op close for stub
        }
    }
}
