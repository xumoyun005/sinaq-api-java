package io.sinaq.jdbc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.*;

class JdbcDbValidatorTest {

    private Connection connection;
    private JdbcDbValidator validator;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:sinaq;DB_CLOSE_DELAY=-1");
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE users (id INT, name VARCHAR(50))");
            st.execute("INSERT INTO users VALUES (1, 'alice')");
        }
        validator = new JdbcDbValidator(connection);
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void assertsRowCountAndScalar() {
        validator.assertRowCount("SELECT * FROM users", 1);
        validator.assertScalar("SELECT name FROM users WHERE id=1", "alice");
        validator.assertExists("SELECT * FROM users WHERE id=1");
    }
}
