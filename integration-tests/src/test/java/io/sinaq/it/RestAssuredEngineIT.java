package io.sinaq.it;

import io.sinaq.api.http.HttpEngine;
import io.sinaq.restassured.RestAssuredEngine;

/** Contract run for the RestAssured engine — same suite, different transport. */
class RestAssuredEngineIT extends EngineContract {
    @Override
    protected HttpEngine engine() {
        return new RestAssuredEngine();
    }
}
