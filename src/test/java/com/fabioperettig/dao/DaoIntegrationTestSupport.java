package com.fabioperettig.dao;

import com.fabioperettig.config.ConnectionFactory;
import com.fabioperettig.config.DatabaseConfig;
import com.fabioperettig.config.SchemaInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class DaoIntegrationTestSupport {

    protected static ConnectionFactory connectionFactory;

    @BeforeAll
    static void initializeDatabase() {
        connectionFactory = DatabaseConfig.load().createTestConnectionFactory();
        new SchemaInitializer(connectionFactory).initialize();
    }

    @BeforeEach
    void cleanDatabaseBeforeTest() throws SQLException {
        cleanDatabase();
    }

    @AfterEach
    void cleanDatabaseAfterTest() throws SQLException {
        cleanDatabase();
    }

    private void cleanDatabase() throws SQLException {
        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "TRUNCATE TABLE tb_sale_item, tb_sale, tb_stock, tb_client, tb_product"
            );
        }
    }
}
