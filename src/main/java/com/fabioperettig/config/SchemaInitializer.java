package com.fabioperettig.config;

import com.fabioperettig.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class SchemaInitializer {

    private static final List<String> SCHEMA_RESOURCES = List.of(
            "database/schemaClient.sql",
            "database/schemaProduct.sql",
            "database/schemaSale.sql",
            "database/schemaSaleItem.sql",
            "database/schemaStock.sql"
    );

    private final ConnectionFactory connectionFactory;

    public SchemaInitializer(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void initialize() {
        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);

            try {
                for (String resource : SCHEMA_RESOURCES) {
                    executeScript(connection, resource);
                }

                connection.commit();
            } catch (IOException | SQLException exception) {
                rollback(connection, exception);
                throw new DataAccessException(
                        "Failed to initialize database schema",
                        exception
                );
            }
        } catch (SQLException exception) {
            throw new DataAccessException(
                    "Failed to connect while initializing database schema",
                    exception
            );
        }
    }

    private void executeScript(Connection connection, String resource)
            throws IOException, SQLException {
        String script = readResource(resource);

        try (Statement statement = connection.createStatement()) {
            for (String command : script.split(";")) {
                if (!command.isBlank()) {
                    statement.execute(command);
                }
            }
        }
    }

    private String readResource(String resource) throws IOException {
        ClassLoader classLoader = SchemaInitializer.class.getClassLoader();

        try (InputStream input = classLoader.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Schema resource not found: " + resource);
            }

            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void rollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }
}
