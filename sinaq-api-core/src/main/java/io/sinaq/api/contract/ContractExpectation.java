package io.sinaq.api.contract;

import java.util.Objects;
import java.util.Optional;

/** Expected shape of a recorded HTTP exchange (V3). */
public record ContractExpectation(
        Optional<String> method,
        Optional<String> urlContains,
        Optional<Integer> status,
        Optional<String> requestBodyContains,
        Optional<String> responseBodyContains) {

    public ContractExpectation {
        method = method == null ? Optional.empty() : method;
        urlContains = urlContains == null ? Optional.empty() : urlContains;
        status = status == null ? Optional.empty() : status;
        requestBodyContains = requestBodyContains == null ? Optional.empty() : requestBodyContains;
        responseBodyContains = responseBodyContains == null ? Optional.empty() : responseBodyContains;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String method;
        private String urlContains;
        private Integer status;
        private String requestBodyContains;
        private String responseBodyContains;

        public Builder method(String v) { method = v; return this; }
        public Builder urlContains(String v) { urlContains = v; return this; }
        public Builder status(int v) { status = v; return this; }
        public Builder requestBodyContains(String v) { requestBodyContains = v; return this; }
        public Builder responseBodyContains(String v) { responseBodyContains = v; return this; }

        public ContractExpectation build() {
            return new ContractExpectation(
                    Optional.ofNullable(method),
                    Optional.ofNullable(urlContains),
                    Optional.ofNullable(status),
                    Optional.ofNullable(requestBodyContains),
                    Optional.ofNullable(responseBodyContains));
        }
    }
}
