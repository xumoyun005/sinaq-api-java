package io.sinaq.api.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MultipartBodyTest {

    @Test
    void buildsMultipartWithTextAndFile() {
        HttpBody body = MultipartBody.builder()
                .part("name", "demo")
                .file("file", "bytes".getBytes(), "demo.txt")
                .build();
        assertThat(body.contentType().orElse("")).contains("multipart/form-data");
        String raw = body.asString();
        assertThat(raw).contains("name=\"name\"");
        assertThat(raw).contains("demo");
        assertThat(raw).contains("filename=\"demo.txt\"");
    }
}
