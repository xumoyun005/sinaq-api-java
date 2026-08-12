package io.sinaq.api.http;

import io.sinaq.api.exception.SinaqEngineException;

/**
 * Transport SPI of the Sinaq framework.
 *
 * <p>An engine translates a Sinaq {@link HttpRequest} into a native call
 * (RestAssured, JDK HttpClient, OkHttp, ...) and translates the native result
 * back into a Sinaq {@link HttpResponse}. Nothing more: no assertions, no
 * serialization decisions, no configuration reading, no logging policy.</p>
 *
 * <p>Contract:</p>
 * <ul>
 *   <li>Must be thread-safe: one engine instance is shared by parallel tests.</li>
 *   <li>Must never leak engine-native types through this SPI.</li>
 *   <li>Must never mutate global/static state of the underlying library.</li>
 *   <li>Transport failures are reported as {@link SinaqEngineException}
 *       (timeouts as {@code SinaqTimeoutException}); an HTTP response with an
 *       error status (4xx/5xx) is NOT an exception — it is a normal response.</li>
 * </ul>
 */
public interface HttpEngine extends AutoCloseable {

    /**
     * Executes the request and blocks until the response is fully received.
     *
     * @throws SinaqEngineException on transport failure
     */
    HttpResponse execute(HttpRequest request);

    /** Stable engine identifier, e.g. {@code "restassured"}, {@code "jdk"}. */
    String name();

    /** Releases engine resources. Default: no-op. */
    @Override
    default void close() {
        // no resources by default
    }
}
