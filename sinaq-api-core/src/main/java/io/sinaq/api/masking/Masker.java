package io.sinaq.api.masking;

import io.sinaq.api.http.HttpHeaders;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Masks sensitive values before they reach logs, events or reports (spec §28).
 *
 * <p>Rule of the framework: masking happens BEFORE a {@code ReportEvent} payload
 * is created — no listener may ever observe an unmasked secret.</p>
 *
 * <p>Default sensitive names (case-insensitive substring match on field/header
 * names): authorization, password, token, refreshToken, accessToken, secret,
 * apiKey/x-api-key, pin, cvv, cvc, otp, smsCode. Additionally any 13–19 digit
 * run (possible card number/PAN) inside text is masked — deliberately
 * aggressive: a masked test id is cheaper than a leaked card number.</p>
 *
 * <p>Immutable; thread-safe. Build custom instances via {@link #builder()}.</p>
 */
public final class Masker {

    public static final String MASK = "****";

    private static final List<String> DEFAULT_NAME_MARKERS = List.of(
            "authorization", "password", "token", "secret",
            "apikey", "api-key", "api_key", "pin", "cvv", "cvc", "otp", "smscode", "sms_code");

    /** 13–19 consecutive digits, optionally space/dash separated (candidate PAN). */
    private static final Pattern PAN = Pattern.compile("\\b\\d(?:[ -]?\\d){12,18}\\b");

    /** Auth scheme credentials anywhere in text (e.g. echoed back in a body). */
    private static final Pattern BEARER_VALUE =
            Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]+");
    private static final Pattern BASIC_VALUE =
            Pattern.compile("(?i)\\bBasic\\s+[A-Za-z0-9+/=]+");

    private static final Masker DEFAULTS = builder().build();

    private final List<String> nameMarkers;         // lower-case
    private final List<Pattern> valuePatterns;      // masked wherever they match in text
    private final Pattern jsonFieldPattern;         // built from nameMarkers

    private Masker(List<String> nameMarkers, List<Pattern> valuePatterns) {
        this.nameMarkers = nameMarkers;
        this.valuePatterns = valuePatterns;
        this.jsonFieldPattern = buildJsonFieldPattern(nameMarkers);
    }

    /** Masker with the default rules. */
    public static Masker defaults() {
        return DEFAULTS;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** True if a field/header with this name must have its value masked. */
    public boolean isSensitiveName(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase(Locale.ROOT);
        return nameMarkers.stream().anyMatch(n::contains);
    }

    /**
     * Masks a named value. Sensitive names keep a recognizable auth scheme prefix
     * ({@code Bearer ****}, {@code Basic ****}); everything else becomes {@code ****}.
     * Non-sensitive names are still scanned for PAN/value patterns.
     */
    public String maskValue(String name, String value) {
        if (value == null) {
            return null;
        }
        if (isSensitiveName(name)) {
            String trimmed = value.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith("bearer ")) {
                return trimmed.substring(0, "Bearer".length()) + " " + MASK;
            }
            if (lower.startsWith("basic ")) {
                return trimmed.substring(0, "Basic".length()) + " " + MASK;
            }
            return MASK;
        }
        return maskText(value);
    }

    /** Masks sensitive JSON fields and PAN-like digit runs inside free text/JSON. */
    public String maskText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String out = jsonFieldPattern.matcher(text).replaceAll("$1" + MASK + "$3");
        out = BEARER_VALUE.matcher(out).replaceAll("Bearer " + MASK);
        out = BASIC_VALUE.matcher(out).replaceAll("Basic " + MASK);
        out = PAN.matcher(out).replaceAll(MASK);
        for (Pattern p : valuePatterns) {
            out = p.matcher(out).replaceAll(MASK);
        }
        return out;
    }

    /** Masked copy of headers as an ordered name→values map (for events/logs). */
    public Map<String, List<String>> maskHeaders(HttpHeaders headers) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        headers.asMap().forEach((name, values) -> {
            List<String> masked = values.stream().map(v -> maskValue(name, v)).toList();
            out.put(name, masked);
        });
        return java.util.Collections.unmodifiableMap(out);
    }

    private static Pattern buildJsonFieldPattern(List<String> markers) {
        // ("name" :) "value"  ->  $1 **** $3   for any field whose name contains a marker
        String alternation = String.join("|", markers.stream().map(Pattern::quote).toList());
        return Pattern.compile(
                "(\"[^\"]*(?i:" + alternation + ")[^\"]*\"\\s*:\\s*\")([^\"]*)(\")");
    }

    /** Mutable builder. NOT thread-safe; confine to one thread. */
    public static final class Builder {

        private final List<String> markers = new ArrayList<>(DEFAULT_NAME_MARKERS);
        private final List<Pattern> valuePatterns = new ArrayList<>();

        private Builder() {}

        /** Adds a custom sensitive name marker (case-insensitive substring). */
        public Builder sensitiveName(String marker) {
            Objects.requireNonNull(marker, "marker");
            markers.add(marker.toLowerCase(Locale.ROOT));
            return this;
        }

        /** Adds a custom regex; every match in scanned text is replaced with the mask. */
        public Builder valuePattern(Pattern pattern) {
            valuePatterns.add(Objects.requireNonNull(pattern, "pattern"));
            return this;
        }

        public Masker build() {
            return new Masker(List.copyOf(markers), List.copyOf(valuePatterns));
        }
    }
}
