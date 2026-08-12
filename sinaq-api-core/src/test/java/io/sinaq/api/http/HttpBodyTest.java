package io.sinaq.api.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class HttpBodyTest {

    @Test
    void jsonFactorySetsContentTypeAndUtf8Bytes() {
        HttpBody b = HttpBody.ofJson("{\"ok\":true}");
        assertThat(b.contentType()).contains(HttpBody.JSON);
        assertThat(b.asString()).isEqualTo("{\"ok\":true}");
        assertThat(b.length()).isEqualTo(11);
    }

    @Test
    void bytesAreDefensivelyCopiedBothWays() {
        byte[] source = {1, 2, 3};
        HttpBody b = HttpBody.ofBytes(source, "application/octet-stream");
        source[0] = 99;                       // mutate input after construction
        byte[] out = b.bytes();
        assertThat(out).containsExactly(1, 2, 3);
        out[1] = 99;                          // mutate returned copy
        assertThat(b.bytes()).containsExactly(1, 2, 3);
    }

    @Test
    void emptyBody() {
        assertThat(HttpBody.empty().isEmpty()).isTrue();
        assertThat(HttpBody.empty().contentType()).isEmpty();
        assertThat(HttpBody.empty().asString()).isEmpty();
    }

    @Test
    void utf8RoundTripForNonLatinText() {
        String uz = "Salom, dunyo — sinaq o'tdi";
        assertThat(HttpBody.ofText(uz).asString()).isEqualTo(uz);
    }
}
