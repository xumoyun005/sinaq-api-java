package io.sinaq.jdbc;

import io.sinaq.api.exception.SinaqAssertionException;
import io.sinaq.api.validation.DbValidator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * JDBC {@link DbValidator} (V3).
 */
public final class JdbcDbValidator implements DbValidator {

    private final Connection connection;

    public JdbcDbValidator(String jdbcUrl) {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        try {
            this.connection = DriverManager.getConnection(jdbcUrl);
        } catch (SQLException e) {
            throw new io.sinaq.api.exception.SinaqConfigurationException(
                    "Failed to open JDBC connection: " + jdbcUrl, e);
        }
    }

    public JdbcDbValidator(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    @Override
    public void assertRowCount(String sql, int expectedRows) {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            int count = 0;
            while (rs.next()) {
                count++;
            }
            if (count != expectedRows) {
                throw new SinaqAssertionException(
                        "DB row count mismatch — expected: " + expectedRows + ", actual: " + count);
            }
        } catch (SQLException e) {
            throw new io.sinaq.api.exception.SinaqConfigurationException("SQL failed: " + sql, e);
        }
    }

    @Override
    public void assertExists(String sql) {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (!rs.next()) {
                throw new SinaqAssertionException("DB query returned no rows: " + sql);
            }
        } catch (SQLException e) {
            throw new io.sinaq.api.exception.SinaqConfigurationException("SQL failed: " + sql, e);
        }
    }

    @Override
    public void assertScalar(String sql, Object expected) {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (!rs.next()) {
                throw new SinaqAssertionException("DB scalar query returned no rows: " + sql);
            }
            Object actual = rs.getObject(1);
            if (!Objects.equals(expected, actual) && !String.valueOf(expected).equals(String.valueOf(actual))) {
                throw new SinaqAssertionException(
                        "DB scalar mismatch — expected: " + expected + ", actual: " + actual);
            }
        } catch (SQLException e) {
            throw new io.sinaq.api.exception.SinaqConfigurationException("SQL failed: " + sql, e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new io.sinaq.api.exception.SinaqConfigurationException("Failed to close JDBC connection", e);
        }
    }
}
