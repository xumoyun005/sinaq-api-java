package io.sinaq.api.http;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable request/response body: raw bytes plus an optional content type.
 * Byte arrays are defensively copied on the way in and out. Thread-safe.
 */
public final class HttpBody {

    public static final String JSON = "application/json";
    public static final String TEXT = "text/plain";
    public static final String FORM = "application/x-www-form-urlencoded";

    private static final HttpBody EMPTY = new HttpBody(new byte[0], null);

    private final byte[] content;
    private final String contentType;

    private HttpBody(byte[] content, String contentType) {
        this.content = content;
        this.contentType = contentType;
    }

    public static HttpBody empty() {
        return EMPTY;
    }

    public static HttpBody ofJson(String json) {
        Objects.requireNonNull(json, "json");
        return new HttpBody(json.getBytes(StandardCharsets.UTF_8), JSON);
    }

    public static HttpBody ofText(String text) {
        Objects.requireNonNull(text, "text");
        return new HttpBody(text.getBytes(StandardCharsets.UTF_8), TEXT);
    }

    public static HttpBody ofBytes(byte[] bytes, String contentType) {
        Objects.requireNonNull(bytes, "bytes");
        return new HttpBody(Arrays.copyOf(bytes, bytes.length), contentType);
    }

    /** Copy of the raw bytes. */
    public byte[] bytes() {
        return Arrays.copyOf(content, content.length);
    }

    /** Body decoded as UTF-8 text. */
    public String asString() {
        return new String(content, StandardCharsets.UTF_8);
    }

    public Optional<String> contentType() {
        return Optional.ofNullable(contentType);
    }

    public int length() {
        return content.length;
    }

    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public String toString() {
        return "HttpBody[type=" + contentType + ", length=" + content.length + "]";
    }
}
