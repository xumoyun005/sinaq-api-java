package io.sinaq.api.http;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class ImmutableHttpResponseTest {

    @Test
    void copiesAndExposesFields() {
        HttpHeaders headers = HttpHeaders.builder().add("X-Test", "1").build();
        ImmutableHttpResponse r = new ImmutableHttpResponse(
                201, Optional.of("Created"), headers,
                List.of(HttpCookie.of("sid", "abc")),
                "body".getBytes(), Duration.ofMillis(5),
                Map.of("engine", "test"));
        ImmutableHttpResponse copy = ImmutableHttpResponse.copyOf(r);
        assertThat(copy.statusCode()).isEqualTo(201);
        assertThat(copy.statusText()).contains("Created");
        assertThat(copy.headers().first("X-Test")).contains("1");
        assertThat(copy.cookies()).hasSize(1);
        assertThat(copy.rawBody()).isEqualTo("body".getBytes());
        assertThat(copy.responseTime()).isEqualTo(Duration.ofMillis(5));
        assertThat(copy.engineMetadata()).containsEntry("engine", "test");
    }
}
