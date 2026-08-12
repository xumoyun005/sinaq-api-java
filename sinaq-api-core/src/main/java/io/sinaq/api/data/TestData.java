package io.sinaq.api.data;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lightweight test-data helpers (V2) — no external Faker dependency.
 */
public final class TestData {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";

    private TestData() {}

    public static String uniqueId() {
        return UUID.randomUUID().toString();
    }

    public static String uniqueId(String prefix) {
        return prefix + "-" + uniqueId().substring(0, 8);
    }

    public static String randomEmail() {
        return "user+" + uniqueId().substring(0, 8) + "@example.test";
    }

    public static String randomPhone() {
        return "+99890" + randomDigits(7);
    }

    public static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public static String randomAlpha(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHA.charAt(ThreadLocalRandom.current().nextInt(ALPHA.length())));
        }
        return sb.toString();
    }

    public static String todayIso() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
