package io.sinaq.api.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class HttpCookieTest {

    @Test
    void simpleFactory() {
        HttpCookie c = HttpCookie.of("session", "abc");
        assertThat(c.name()).isEqualTo("session");
        assertThat(c.value()).isEqualTo("abc");
        assertThat(c.secure()).isFalse();
        assertThat(c.httpOnly()).isFalse();
        assertThat(c.domain()).isNull();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> HttpCookie.of("  ", "v"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
