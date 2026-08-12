package io.sinaq.wiremock;

import io.sinaq.api.client.Sinaq;
import io.sinaq.jdk.JdkHttpEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SinaqWireMockTest {

    private SinaqWireMock wireMock;

    @AfterEach
    void tearDown() {
        if (wireMock != null) {
            wireMock.close();
        }
    }

    @Test
    void stubsAndServesJson() {
        wireMock = new SinaqWireMock();
        wireMock.stubGet("/health", 200, "{\"status\":\"UP\"}");
        var api = Sinaq.emptyClient().baseUrl(wireMock.baseUrl()).engine(new JdkHttpEngine()).build();
        api.get("/health").expectStatus(200).expect("status", "UP");
        assertThat(wireMock.requestCount("/health")).isEqualTo(1);
    }
}
