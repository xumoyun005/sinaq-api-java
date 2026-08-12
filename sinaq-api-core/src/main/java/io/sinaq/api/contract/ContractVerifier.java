package io.sinaq.api.contract;

import io.sinaq.api.exception.SinaqAssertionException;
import io.sinaq.api.recording.RecordedExchange;

import java.util.List;
import java.util.Objects;

/** Verifies recorded exchanges against expectations (V3 contract testing). */
public final class ContractVerifier {

    private ContractVerifier() {}

    public static void verify(RecordedExchange exchange, ContractExpectation expected) {
        Objects.requireNonNull(exchange, "exchange");
        Objects.requireNonNull(expected, "expected");
        expected.method().ifPresent(m -> {
            if (!exchange.method().equalsIgnoreCase(m)) {
                fail("method", m, exchange.method());
            }
        });
        expected.urlContains().ifPresent(part -> {
            if (!exchange.url().contains(part)) {
                fail("url contains", part, exchange.url());
            }
        });
        expected.status().ifPresent(s -> {
            if (exchange.status() != s) {
                fail("status", s, exchange.status());
            }
        });
        expected.requestBodyContains().ifPresent(part -> {
            if (!exchange.requestBody().contains(part)) {
                fail("request body contains", part, preview(exchange.requestBody()));
            }
        });
        expected.responseBodyContains().ifPresent(part -> {
            if (!exchange.responseBody().contains(part)) {
                fail("response body contains", part, preview(exchange.responseBody()));
            }
        });
    }

    public static void verifyAny(List<RecordedExchange> exchanges, ContractExpectation expected) {
        Objects.requireNonNull(exchanges, "exchanges");
        for (RecordedExchange ex : exchanges) {
            try {
                verify(ex, expected);
                return;
            } catch (SinaqAssertionException ignored) {
                // try next
            }
        }
        throw new SinaqAssertionException("No recorded exchange matched contract: " + expected);
    }

    private static void fail(String field, Object expected, Object actual) {
        throw new SinaqAssertionException(
                "Contract mismatch on " + field + " — expected: " + expected + ", actual: " + actual);
    }

    private static String preview(String s) {
        return s.length() <= 256 ? s : s.substring(0, 256) + "...";
    }
}
