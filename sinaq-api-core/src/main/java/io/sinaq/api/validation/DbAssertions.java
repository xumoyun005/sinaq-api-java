package io.sinaq.api.validation;

import java.util.Objects;

/** Fluent DB assertion helper (V3). */
public final class DbAssertions {

    private final DbValidator validator;

    public DbAssertions(DbValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public DbAssertions rowCount(String sql, int expected) {
        validator.assertRowCount(sql, expected);
        return this;
    }

    public DbAssertions exists(String sql) {
        validator.assertExists(sql);
        return this;
    }

    public DbAssertions scalar(String sql, Object expected) {
        validator.assertScalar(sql, expected);
        return this;
    }
}
