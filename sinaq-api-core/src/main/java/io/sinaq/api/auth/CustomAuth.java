package io.sinaq.api.auth;

import io.sinaq.api.request.RequestSpec;

import java.util.Objects;
import java.util.function.Consumer;

/** Arbitrary auth decoration via a lambda. The lambda must be thread-safe. */
public final class CustomAuth implements AuthProvider {

    private final Consumer<RequestSpec> decorator;

    public CustomAuth(Consumer<RequestSpec> decorator) {
        this.decorator = Objects.requireNonNull(decorator, "decorator");
    }

    @Override
    public void apply(RequestSpec spec) {
        decorator.accept(spec);
    }
}
