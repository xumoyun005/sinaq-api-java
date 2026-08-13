package io.sinaq.examples;

import io.sinaq.api.client.ApiClient;
import io.sinaq.api.client.Sinaq;
import io.sinaq.api.response.ApiResponse;
import io.sinaq.jdk.JdkHttpEngine;
import io.sinaq.jsonpath.JaywayExtractionProvider;
import io.sinaq.junit5.SinaqExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Five everyday RestAssured patterns → Sinaq.
 * <p>
 * Each test shows the RestAssured equivalent in a comment, then the runnable Sinaq version.
 * Full guide: {@code docs/migrate-from-restassured.md}.
 */
@ExtendWith(SinaqExtension.class)
class RestAssuredMigrationExample {

    private static ExampleServer server;
    private static ApiClient api;

    @BeforeAll
    static void setUp() throws Exception {
        server = new ExampleServer();
        api = Sinaq.emptyClient()
                .baseUrl(server.baseUrl())
                .engine(new JdkHttpEngine())
                .plugin(new JaywayExtractionProvider.Plugin())
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    /**
     * RestAssured:
     * <pre>{@code
     * given()
     *   .queryParam("page", 1)
     * .when()
     *   .get("/get")
     * .then()
     *   .statusCode(200)
     *   .body("page", equalTo("1"));
     * }</pre>
     */
    @Test
    void pair1_getWithQueryAndStatus() {
        api.get("/get")
                .queryParam("page", 1)
                .expectStatus(200)
                .expect("page", "1");
    }

    /**
     * RestAssured:
     * <pre>{@code
     * given()
     *   .auth().oauth2("demo-token")
     *   .contentType("application/json")
     *   .body(Map.of("amount", 150, "currency", "UZS"))
     * .when()
     *   .post("/post")
     * .then()
     *   .statusCode(200);
     * }</pre>
     */
    @Test
    void pair2_postBearerAndJsonBody() {
        api.post("/post")
                .bearer("demo-token")
                .body(Map.of("amount", 150, "currency", "UZS"))
                .expectStatus(200)
                .expectContains("demo-token");
    }

    /**
     * RestAssured:
     * <pre>{@code
     * String origin = given()
     * .when()
     *   .get("/get")
     * .then()
     *   .statusCode(200)
     *   .extract()
     *   .path("origin");
     * }</pre>
     */
    @Test
    void pair3_extractPathForNextStep() {
        String origin = api.get("/get").execute().extract("$.origin");
        assertThat(origin).isEqualTo("127.0.0.1");
    }

    /**
     * RestAssured:
     * <pre>{@code
     * given()
     *   .header("X-Request-Id", "abc-123")
     * .when()
     *   .get("/get")
     * .then()
     *   .statusCode(200);
     * }</pre>
     */
    @Test
    void pair4_customRequestHeader() {
        api.get("/get")
                .header("X-Request-Id", "abc-123")
                .expectStatus(200);
    }

    /**
     * RestAssured:
     * <pre>{@code
     * given()
     * .when()
     *   .get("/cards")
     * .then()
     *   .statusCode(200)
     *   .body("cards", hasSize(2))
     *   .body("cards[0].uuid", equalTo("u-1"));
     *
     * List<String> ids = ... extract().path("cards.uuid");
     * }</pre>
     */
    @Test
    void pair5_arraySizeAndExtractList() {
        ApiResponse r = api.get("/cards").execute();
        r.expectStatus(200).expectArraySize("$.cards", 2);

        List<String> uuids = r.extractList("$.cards[*].uuid");
        assertThat(uuids).containsExactly("u-1", "u-2");
    }
}
