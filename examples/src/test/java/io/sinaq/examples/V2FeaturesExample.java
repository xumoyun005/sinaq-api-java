package io.sinaq.examples;

import io.sinaq.api.client.ApiClient;
import io.sinaq.api.client.Sinaq;
import io.sinaq.api.data.TestData;
import io.sinaq.api.http.HttpMethod;
import io.sinaq.api.template.RequestTemplate;
import io.sinaq.jsonpath.JaywayExtractionProvider;
import io.sinaq.jsonschema.NetworkntSchemaValidator;
import io.sinaq.jdk.JdkHttpEngine;
import io.sinaq.junit5.SinaqExtension;
import io.sinaq.oauth.OAuth2TokenProvider;
import io.sinaq.oauth.OAuth2TokenProvider.OAuth2Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * V2 showcase: JSONPath plugin, schema validation, polling, templates, OAuth2, soft assertions.
 */
@ExtendWith(SinaqExtension.class)
class V2FeaturesExample {

    private static ExampleServer server;
    private static ApiClient api;

    @BeforeAll
    static void setUp() throws Exception {
        server = new ExampleServer();
        api = Sinaq.emptyClient()
                .baseUrl(server.baseUrl())
                .engine(new JdkHttpEngine())
                .plugin(new JaywayExtractionProvider.Plugin())
                .plugin(new NetworkntSchemaValidator.Plugin())
                .registerTemplate(RequestTemplate.builder("listCards", HttpMethod.GET, "/cards")
                        .header("X-Demo", "v2")
                        .build())
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void jsonPathWildcardExtraction() {
        List<String> uuids = api.fromTemplate("listCards")
                .execute()
                .extractList("$.cards[*].uuid");
        assertThat(uuids).containsExactly("u-1", "u-2");
    }

    @Test
    void pollingUntilReady() {
        api.get("/poll")
                .poll()
                .untilStatus(200)
                .execute()
                .expect("ready", true);
    }

    @Test
    void softAssertionsAndSchema() {
        api.get("/cards")
                .execute()
                .soft()
                .expectStatus(200)
                .expectSchema("{\"type\":\"object\",\"properties\":{\"cards\":{\"type\":\"array\"}}}")
                .expectArraySize("cards", 2)
                .assertAll();
    }

    @Test
    void oauth2BearerToken() {
        OAuth2Config config = OAuth2Config.builder(server.baseUrl() + "/token")
                .clientId("demo")
                .clientSecret("secret")
                .build();
        api.post("/post")
                .bearer(new OAuth2TokenProvider(config).accessToken())
                .body(Map.of("id", TestData.uniqueId()))
                .expectStatus(200)
                .expectContains("demo-oauth-token");
    }
}
