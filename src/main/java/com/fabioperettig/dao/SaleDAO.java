package com.fabioperettig.dao;

import com.fabioperettig.config.ConnectionFactory;
import com.fabioperettig.domain.*;
import com.fabioperettig.exception.DataAccessException;

import java.sql.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SaleDAO {

    private final ConnectionFactory connectionFactory;

    private static final String INSERT_SALE_SQL = """
            INSERT INTO tb_sale (code_sale, client_id, sale_date, status_sale)
            VALUES (?, ?, ?, ?)
            """;

    private static final String INSERT_ITEM_SQL = """
            INSERT INTO tb_sale_item (sale_id, product_id, quantity, unit_price)
            VALUES (?, ?, ?, ?)
            """;

    private static final String FIND_BY_ID_SQL = """
        SELECT s.id, s.code_sale, s.client_id, s.sale_date, s.status_sale,
               c.name_client, c.cpf_client, c.contact_client
        FROM tb_sale s INNER JOIN tb_client c ON c.id = s.client_id WHERE s.id = ?
        """;

    private static final String FIND_ITEMS_SQL = """
        SELECT i.product_id, i.quantity, i.unit_price,
               p.name_product, p.code_product, p.price_product
        FROM tb_sale_item i INNER JOIN tb_product p ON p.id = i.product_id
        WHERE i.sale_id = ? ORDER BY i.product_id
        """;

    public SaleDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(
                connectionFactory, "ConnectionFactory is required"
        );
    }

    private Long insertSale(Connection connection, Sale sale) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_SALE_SQL, Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, sale.getCode());
            statement.setLong(2, sale.getClient().getId());
            statement.setObject(3, sale.getSaleDate().atOffset(ZoneOffset.UTC));
            statement.setString(4, sale.getStatus().name());

            int affectedRows = statement.executeUpdate();

            if (affectedRows != 1) {
                throw new SQLException("Expected one inserted sale row");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Database did not return the sale ID");
                }

                return generatedKeys.getLong(1);
            }
        }
    }

    private void insertItems(Connection connection, Long saleId, Sale sale)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(INSERT_ITEM_SQL)) {

            for (SaleItem item : sale.getItems()) {
                statement.setLong(1, saleId);
                statement.setLong(2, item.getProduct().getId());
                statement.setInt(3, item.getQuantity());
                statement.setBigDecimal(4, item.getUnitPrice());

                int affectedRows = statement.executeUpdate();

                if (affectedRows != 1) {
                    throw new SQLException("Expected one inserted sale item row");
                }
            }
        }
    }

    public Sale create(Sale sale) {
        Objects.requireNonNull(sale, "Sale is required");

        if (sale.getId() != null) {
            throw new IllegalArgumentException("Sale already has an ID");
        }

        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Long saleId = insertSale(connection, sale);
                insertItems(connection, saleId, sale);

                connection.commit();
                sale.setId(saleId);

                return sale;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw new DataAccessException("Failed to create sale", exception);
            }
        } catch (SQLException exception) {
            throw new DataAccessException(
                    "Failed to open or close sale connection", exception
            );
        }
    }

    private List<SaleItem> findItems(Connection connection, Long saleId)
            throws SQLException {

        List<SaleItem> items = new ArrayList<>();

        try (PreparedStatement statement =
                     connection.prepareStatement(FIND_ITEMS_SQL)) {

            statement.setLong(1, saleId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Product product = new Product();
                    product.setId(resultSet.getLong("product_id"));
                    product.setName(resultSet.getString("name_product"));
                    product.setCode(resultSet.getString("code_product"));
                    product.setPrice(resultSet.getBigDecimal("price_product"));

                    SaleItem item = new SaleItem(
                            product,
                            resultSet.getInt("quantity"),
                            resultSet.getBigDecimal("unit_price")
                    );

                    items.add(item);
                }
            }
        }

        return items;
    }

    public Optional<Sale> findById(Long saleId) {
        Objects.requireNonNull(saleId, "Sale ID is required");

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(FIND_BY_ID_SQL)) {

            statement.setLong(1, saleId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                Client client = new Client();
                client.setId(resultSet.getLong("client_id"));
                client.setName(resultSet.getString("name_client"));
                client.setCpf(resultSet.getString("cpf_client"));
                client.setContact(resultSet.getString("contact_client"));

                List<SaleItem> items = findItems(connection, saleId);

                Sale sale = Sale.restore(
                        resultSet.getLong("id"),
                        resultSet.getString("code_sale"),
                        client,
                        resultSet.getObject(
                                "sale_date", OffsetDateTime.class
                        ).toInstant(),
                        SaleStatus.valueOf(resultSet.getString("status_sale")),
                        items
                );

                return Optional.of(sale);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to find sale by ID", exception);
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
