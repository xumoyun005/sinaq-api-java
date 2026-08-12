package io.sinaq.api.http;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds {@code multipart/form-data} bodies (V3).
 */
public final class MultipartBody {

    public static final String MULTIPART = "multipart/form-data";

    private MultipartBody() {}

    public record Part(String name, byte[] content, String contentType, String filename) {
        public Part {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(content, "content");
        }

        public static Part text(String name, String value) {
            return new Part(name, value.getBytes(StandardCharsets.UTF_8), HttpBody.TEXT, null);
        }

        public static Part file(String name, byte[] bytes, String filename, String contentType) {
            return new Part(name, bytes, contentType != null ? contentType : "application/octet-stream",
                    filename);
        }
    }

    public static HttpBody build(List<Part> parts) {
        Objects.requireNonNull(parts, "parts");
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("multipart requires at least one part");
        }
        String boundary = "sinaq-" + UUID.randomUUID();
        byte[] bytes = encode(parts, boundary);
        return HttpBody.ofBytes(bytes, MULTIPART + "; boundary=" + boundary);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Part> parts = new ArrayList<>();

        public Builder part(String name, String value) {
            parts.add(Part.text(name, value));
            return this;
        }

        public Builder file(String name, byte[] bytes, String filename) {
            parts.add(Part.file(name, bytes, filename, "application/octet-stream"));
            return this;
        }

        public Builder file(String name, byte[] bytes, String filename, String contentType) {
            parts.add(Part.file(name, bytes, filename, contentType));
            return this;
        }

        public HttpBody build() {
            return MultipartBody.build(parts);
        }

        public Builder copy() {
            Builder copy = new Builder();
            copy.parts.addAll(parts);
            return copy;
        }
    }

    private static byte[] encode(List<Part> parts, String boundary) {
        String lineEnd = "\r\n";
        String dash = "--";
        List<byte[]> chunks = new ArrayList<>();
        for (Part p : parts) {
            StringBuilder header = new StringBuilder();
            header.append(dash).append(boundary).append(lineEnd);
            header.append("Content-Disposition: form-data; name=\"").append(p.name()).append("\"");
            if (p.filename() != null) {
                header.append("; filename=\"").append(p.filename()).append("\"");
            }
            header.append(lineEnd);
            if (p.contentType() != null) {
                header.append("Content-Type: ").append(p.contentType()).append(lineEnd);
            }
            header.append(lineEnd);
            chunks.add(header.toString().getBytes(StandardCharsets.UTF_8));
            chunks.add(p.content());
            chunks.add(lineEnd.getBytes(StandardCharsets.UTF_8));
        }
        String end = dash + boundary + dash + lineEnd;
        chunks.add(end.getBytes(StandardCharsets.UTF_8));
        int total = 0;
        for (byte[] c : chunks) {
            total += c.length;
        }
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] c : chunks) {
            System.arraycopy(c, 0, out, pos, c.length);
            pos += c.length;
        }
        return out;
    }
}
