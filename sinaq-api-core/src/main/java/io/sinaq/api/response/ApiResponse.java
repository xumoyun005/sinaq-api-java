package io.sinaq.api.response;

import io.sinaq.api.SinaqRuntime;
import io.sinaq.api.assertion.AssertionFailureFormatter;
import io.sinaq.api.assertion.SchemaValidator;
import io.sinaq.api.assertion.SoftAssertions;
import io.sinaq.api.events.EventType;
import io.sinaq.api.events.ReportEvent;
import io.sinaq.api.exception.SinaqAssertionException;
import io.sinaq.api.exception.SinaqSerializationException;
import io.sinaq.api.extraction.ExtractionProvider;
import io.sinaq.api.http.HttpCookie;
import io.sinaq.api.http.HttpRequest;
import io.sinaq.api.http.HttpResponse;
import io.sinaq.api.http.HttpStatus;
import io.sinaq.api.internal.json.Json;
import io.sinaq.api.masking.Masker;
import io.sinaq.api.serialization.SerializationProvider;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Framework-independent response (spec §9) with the fluent assertion API
 * (spec §13, V2 advanced assertions). Assertions are fail-fast by default;
 * use {@link #soft()} for batched failures. Immutable and thread-safe.
 */
public final class ApiResponse {

    private final HttpRequest request;
    private final HttpResponse http;
    private final SerializationProvider serializer;
    private final ExtractionProvider extractor;
    private final SchemaValidator schemaValidator;
    private final Masker masker;

    private volatile Object jsonTree;
    private volatile boolean jsonParsed;

    public ApiResponse(HttpRequest request, HttpResponse http,
                       SerializationProvider serializer,
                       ExtractionProvider extractor,
                       SchemaValidator schemaValidator,
                       Masker masker) {
        this.request = Objects.requireNonNull(request, "request");
        this.http = Objects.requireNonNull(http, "http");
        this.serializer = Objects.requireNonNull(serializer, "serializer");
        this.extractor = Objects.requireNonNull(extractor, "extractor");
        this.schemaValidator = Objects.requireNonNull(schemaValidator, "schemaValidator");
        this.masker = Objects.requireNonNull(masker, "masker");
    }

    // ---------- data access (spec §9) ----------

    public int status()                    { return http.statusCode(); }
    public HttpStatus statusValue()        { return http.status(); }
    public Optional<String> statusText()   { return http.statusText(); }
    public byte[] rawBody()                { return http.rawBody(); }
    public String text()                   { return new String(http.rawBody(), StandardCharsets.UTF_8); }
    public String body()                   { return text(); }
    public Optional<String> header(String name) { return http.headers().first(name); }
    public Map<String, List<String>> headers()  { return http.headers().asMap(); }
    public Optional<HttpCookie> cookie(String name) {
        return http.cookies().stream().filter(c -> c.name().equals(name)).findFirst();
    }
    public Duration responseTime()         { return http.responseTime(); }
    public HttpRequest request()           { return request; }

    public Object jsonPath() {
        if (!jsonParsed) {
            synchronized (this) {
                if (!jsonParsed) {
                    jsonTree = Json.parse(text());
                    jsonParsed = true;
                }
            }
        }
        return jsonTree;
    }

    @SuppressWarnings("unchecked")
    public <T> T extract(String path) {
        return (T) extractor.extract(jsonPath(), path);
    }

    /** Extracts a list at the path (V2). */
    @SuppressWarnings("unchecked")
    public <T> List<T> extractList(String path) {
        Object value = extract(path);
        if (value instanceof List<?> list) {
            return (List<T>) list;
        }
        throw new SinaqSerializationException(
                "Path '" + path + "' did not resolve to a list, got: "
                + (value == null ? "null" : value.getClass().getName()));
    }

    public <T> T as(Class<T> type) {
        try {
            return serializer.deserialize(text(), type);
        } catch (SinaqSerializationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new SinaqSerializationException("Failed to deserialize response as " + type.getName(), e,
                    request.context().correlationId().orElse(null));
        }
    }

    // ---------- assertions (spec §13 + V2) ----------

    public ApiResponse expectStatus(int expected) {
        return check("status == " + expected, expected, status(), status() == expected);
    }

    public ApiResponse expectHeader(String name, String expected) {
        String actual = header(name).orElse(null);
        return check("header '" + name + "' == '" + expected + "'", expected, actual,
                Objects.equals(expected, actual));
    }

    public ApiResponse expect(String path, Object expected) {
        Object actual = extract(path);
        return check("json '" + path + "' == " + expected, expected, actual, valueEquals(expected, actual));
    }

    public ApiResponse expectEquals(String path, Object expected) {
        return expect(path, expected);
    }

    public ApiResponse expectNotNull(String path) {
        Object actual = extract(path);
        return check("json '" + path + "' is not null", "non-null", actual, actual != null);
    }

    public ApiResponse expectNull(String path) {
        Object actual = extract(path);
        return check("json '" + path + "' is null", null, actual, actual == null);
    }

    public ApiResponse expectContains(String substring) {
        Objects.requireNonNull(substring, "substring");
        return check("body contains '" + substring + "'", substring, "<body:" + text().length() + " chars>",
                text().contains(substring));
    }

    /** Asserts body matches regex (V2). */
    public ApiResponse expectMatches(String regex) {
        Objects.requireNonNull(regex, "regex");
        Pattern pattern = Pattern.compile(regex);
        return check("body matches /" + regex + "/", regex, text(), pattern.matcher(text()).find());
    }

    /** Asserts JSON array at path has expected size (V2). */
    public ApiResponse expectArraySize(String path, int expected) {
        List<?> list = extractList(path);
        return check("json '" + path + "' array size == " + expected, expected, list.size(),
                list.size() == expected);
    }

    public ApiResponse expectResponseTimeLessThan(Duration max) {
        Objects.requireNonNull(max, "max");
        return check("responseTime < " + max, max, responseTime(), responseTime().compareTo(max) < 0);
    }

    /** Validates body against JSON Schema (V2 — requires sinaq-api-jsonschema). */
    public ApiResponse expectSchema(String schemaJson) {
        Objects.requireNonNull(schemaJson, "schemaJson");
        try {
            schemaValidator.validate(text(), schemaJson);
            return check("body matches JSON schema", "valid", "valid", true);
        } catch (RuntimeException e) {
            return check("body matches JSON schema", "valid", e.getMessage(), false);
        }
    }

    /** Soft-assert mode — failures collected until {@link SoftAssertions#assertAll()}. */
    public SoftAssertions soft() {
        return new SoftAssertions(this);
    }

    // ---------- internals ----------

    private static boolean valueEquals(Object expected, Object actual) {
        if (expected instanceof Number en && actual instanceof Number an) {
            if (isIntegral(en) && isIntegral(an)) {
                return en.longValue() == an.longValue();
            }
            return Double.compare(en.doubleValue(), an.doubleValue()) == 0;
        }
        return Objects.equals(expected, actual);
    }

    private static boolean isIntegral(Number n) {
        return n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte;
    }

    private ApiResponse check(String description, Object expected, Object actual, boolean passed) {
        publishAssertion(passed, description, expected, actual);
        if (!passed) {
            throw new SinaqAssertionException(
                    AssertionFailureFormatter.format(
                            description, expected, actual, request, http, masker),
                    null, request.context().correlationId().orElse(null));
        }
        return this;
    }

    private void publishAssertion(boolean passed, String description, Object expected, Object actual) {
        var ctx = request.context();
        SinaqRuntime.publisher().publish(ReportEvent.builder(
                        passed ? EventType.ASSERTION_PASSED : EventType.ASSERTION_FAILED,
                        SinaqRuntime.executionContext().executionId())
                .requestId(ctx.requestId())
                .correlationId(ctx.correlationId().orElse(null))
                .testId(currentTestId())
                .payload(Map.of(
                        "assertion", description,
                        "expected", String.valueOf(masker.maskText(String.valueOf(expected))),
                        "actual", String.valueOf(masker.maskText(String.valueOf(actual)))))
                .build());
    }

    private static String currentTestId() {
        var t = io.sinaq.api.context.SinaqTestContextHolder.get();
        return t == null ? null : t.testId();
    }
}
