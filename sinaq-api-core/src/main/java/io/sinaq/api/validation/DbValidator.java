package io.sinaq.api.validation;

/**
 * Database validation SPI (V3). Implementations live in adapter modules (e.g. JDBC).
 */
public interface DbValidator extends AutoCloseable {

    /** Asserts SQL returns exactly {@code expectedRows} rows. */
    void assertRowCount(String sql, int expectedRows);

    /** Asserts SQL returns at least one row. */
    void assertExists(String sql);

    /** Asserts a single scalar column value. */
    void assertScalar(String sql, Object expected);

    @Override
    default void close() {}
}
