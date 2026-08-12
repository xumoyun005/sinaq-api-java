package io.sinaq.api.masking;

import io.sinaq.api.http.HttpHeaders;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

class MaskerTest {

    private final Masker masker = Masker.defaults();

    @Test
    void sensitiveHeaderNamesAreDetected() {
        assertThat(masker.isSensitiveName("Authorization")).isTrue();
        assertThat(masker.isSensitiveName("X-Api-Key")).isTrue();
        assertThat(masker.isSensitiveName("refreshToken")).isTrue();
        assertThat(masker.isSensitiveName("Content-Type")).isFalse();
    }

    @Test
    void bearerSchemeIsKeptTokenIsMasked() {
        assertThat(masker.maskValue("Authorization", "Bearer eyJhbGciOi.secret"))
                .isEqualTo("Bearer ****");
        assertThat(masker.maskValue("Authorization", "Basic dXNlcjpwYXNz"))
                .isEqualTo("Basic ****");
        assertThat(masker.maskValue("X-Api-Key", "abc123")).isEqualTo("****");
    }

    @Test
    void jsonSensitiveFieldsAreMasked() {
        String json = "{\"login\":\"ali\",\"password\":\"p@ss\",\"accessToken\":\"tok-1\",\"smsCode\":\"1234\"}";
        String masked = masker.maskText(json);
        assertThat(masked).contains("\"password\":\"****\"");
        assertThat(masked).contains("\"accessToken\":\"****\"");
        assertThat(masked).contains("\"smsCode\":\"****\"");
        assertThat(masked).contains("\"login\":\"ali\"");
    }

    @Test
    void panLikeDigitRunsAreMaskedInAnyText() {
        assertThat(masker.maskText("card 8600 1234 5678 9012 charged"))
                .isEqualTo("card **** charged");
        assertThat(masker.maskText("id=12345 ok")).contains("12345"); // short runs untouched
    }

    @Test
    void maskHeadersProducesMaskedCopy() {
        HttpHeaders h = HttpHeaders.builder()
                .add("Authorization", "Bearer tok")
                .add("X-App-Version", "1.0.3")
                .build();
        Map<String, List<String>> masked = masker.maskHeaders(h);
        assertThat(masked.get("Authorization")).containsExactly("Bearer ****");
        assertThat(masked.get("X-App-Version")).containsExactly("1.0.3");
        assertThat(h.first("Authorization")).contains("Bearer tok"); // original untouched
    }

    @Test
    void customMarkerAndPattern() {
        Masker custom = Masker.builder()
                .sensitiveName("cardUuid")
                .valuePattern(Pattern.compile("SECRET-\\d+"))
                .build();
        assertThat(custom.maskValue("cardUuid", "abc")).isEqualTo("****");
        assertThat(custom.maskText("value SECRET-42 here")).isEqualTo("value **** here");
    }
}
