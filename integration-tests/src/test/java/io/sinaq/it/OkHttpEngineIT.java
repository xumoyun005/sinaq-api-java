package io.sinaq.it;

import io.sinaq.api.http.HttpEngine;
import io.sinaq.okhttp.OkHttpEngine;

/** Contract run for the OkHttp engine. */
class OkHttpEngineIT extends EngineContract {
    @Override
    protected HttpEngine engine() {
        return new OkHttpEngine();
    }
}
