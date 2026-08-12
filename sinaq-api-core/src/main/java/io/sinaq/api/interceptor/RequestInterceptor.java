package io.sinaq.api.interceptor;

import io.sinaq.api.http.HttpResponse;
import io.sinaq.api.request.ApiRequest;

/**
 * Hooks around transport execution (V3). Interceptors run in registration order;
 * {@link #afterResponse} runs in reverse order.
 */
public interface RequestInterceptor {

    /** Called before the engine executes the request. May return a modified snapshot. */
    default ApiRequest beforeRequest(ApiRequest request) {
        return request;
    }

    /** Called after a successful engine response. May return a modified response. */
    default HttpResponse afterResponse(ApiRequest request, HttpResponse response) {
        return response;
    }
}
