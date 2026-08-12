package io.sinaq.api.validation;

import io.sinaq.api.exception.SinaqAssertionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DbAssertionsTest {

    @Test
    void delegatesToValidator() {
        DbValidator stub = new DbValidator() {
            @Override
            public void assertRowCount(String sql, int expectedRows) {
                assertThat(sql).contains("users");
                assertThat(expectedRows).isEqualTo(2);
            }
            @Override
            public void assertExists(String sql) {
                assertThat(sql).contains("exists");
            }
            @Override
            public void assertScalar(String sql, Object expected) {
                assertThat(expected).isEqualTo(1);
            }
        };
        new DbAssertions(stub)
                .rowCount("SELECT * FROM users", 2)
                .exists("SELECT exists")
                .scalar("SELECT 1", 1);
    }

    @Test
    void propagatesAssertionFailures() {
        DbValidator stub = new DbValidator() {
            @Override
            public void assertRowCount(String sql, int expectedRows) {
                throw new SinaqAssertionException("fail");
            }
            @Override
            public void assertExists(String sql) {}
            @Override
            public void assertScalar(String sql, Object expected) {}
        };
        assertThatThrownBy(() -> new DbAssertions(stub).rowCount("x", 1))
                .isInstanceOf(SinaqAssertionException.class);
    }
}
