package io.sinaq.it;

import io.sinaq.api.http.HttpEngine;
import io.sinaq.jdk.JdkHttpEngine;

/** Contract run for the JDK engine. */
class JdkEngineIT extends EngineContract {
    @Override
    protected HttpEngine engine() {
        return new JdkHttpEngine();
    }
}
