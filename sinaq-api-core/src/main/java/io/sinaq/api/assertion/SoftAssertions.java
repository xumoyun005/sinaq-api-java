package io.sinaq.api.assertion;

import io.sinaq.api.exception.SinaqAssertionException;
import io.sinaq.api.response.ApiResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Collects assertion failures without failing immediately (V2 soft-assert mode).
 *
 * <pre>{@code
 * response.soft()
 *     .expectStatus(200)
 *     .expect("id", 42)
 *     .assertAll();
 * }</pre>
 */
public final class SoftAssertions {

    private final ApiResponse response;
    private final List<String> failures = new ArrayList<>();

    public SoftAssertions(ApiResponse response) {
        this.response = response;
    }

    public SoftAssertions expectStatus(int expected) {
        return run(() -> response.expectStatus(expected));
    }

    public SoftAssertions expectHeader(String name, String expected) {
        return run(() -> response.expectHeader(name, expected));
    }

    public SoftAssertions expect(String path, Object expected) {
        return run(() -> response.expect(path, expected));
    }

    public SoftAssertions expectEquals(String path, Object expected) {
        return run(() -> response.expectEquals(path, expected));
    }

    public SoftAssertions expectNotNull(String path) {
        return run(() -> response.expectNotNull(path));
    }

    public SoftAssertions expectNull(String path) {
        return run(() -> response.expectNull(path));
    }

    public SoftAssertions expectContains(String substring) {
        return run(() -> response.expectContains(substring));
    }

    public SoftAssertions expectMatches(String regex) {
        return run(() -> response.expectMatches(regex));
    }

    public SoftAssertions expectArraySize(String path, int expected) {
        return run(() -> response.expectArraySize(path, expected));
    }

    public SoftAssertions expectResponseTimeLessThan(Duration max) {
        return run(() -> response.expectResponseTimeLessThan(max));
    }

    public SoftAssertions expectSchema(String schemaJson) {
        return run(() -> response.expectSchema(schemaJson));
    }

    /** Fails with a combined message if any soft assertion failed. */
    public void assertAll() {
        if (!failures.isEmpty()) {
            throw new SinaqAssertionException(
                    failures.size() + " soft assertion(s) failed:\n- "
                    + String.join("\n- ", failures));
        }
    }

    public List<String> failures() {
        return List.copyOf(failures);
    }

    private SoftAssertions run(Runnable assertion) {
        try {
            assertion.run();
        } catch (SinaqAssertionException e) {
            failures.add(e.getMessage());
        }
        return this;
    }
}
