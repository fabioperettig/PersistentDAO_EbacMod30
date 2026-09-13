package com.fabioperettig.dao;

import com.fabioperettig.config.ConnectionFactory;
import com.fabioperettig.domain.Stock;
import com.fabioperettig.exception.DataAccessException;
import com.fabioperettig.domain.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StockDAO implements IGenericDAO<Stock, Long> {

    private final ConnectionFactory connectionFactory;

    private static final String INSERT_SQL =
            "INSERT INTO tb_stock (product_id, available_quantity) VALUES (?, ?)";

    private static final String FIND_BY_ID_SQL = """
        SELECT s.product_id, s.available_quantity, p.name_product, p.code_product, p.price_product
        FROM tb_stock s INNER JOIN tb_product p ON p.id = s.product_id WHERE s.product_id = ?
        """;

    private static final String FIND_ALL_SQL = """
        SELECT s.product_id, s.available_quantity, p.name_product, p.code_product, p.price_product
        FROM tb_stock s INNER JOIN tb_product p ON p.id = s.product_id ORDER BY s.product_id
        """;

    private static final String UPDATE_SQL =
            "UPDATE tb_stock SET available_quantity = ? WHERE product_id = ?";

    private static final String DELETE_BY_ID_SQL =
            "DELETE FROM tb_stock WHERE product_id = ?";

    public StockDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(
                connectionFactory, "ConnectionFactory is required"
        );
    }


    @Override
    public Stock create(Stock stock) {
        Objects.requireNonNull(stock, "Stock is required");

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     INSERT_SQL))
        {
            bindInsert(statement, stock);
            int affectedRows = statement.executeUpdate();
            if (affectedRows != 1) {
                throw new DataAccessException("Expected one insert row, but got: " + affectedRows);
            }

            return stock;

        } catch (SQLException exception) {
            throw new DataAccessException("Failed to create stock", exception);
        }
    }

    @Override
    public Optional<Stock> findById(Long productId) {
        Objects.requireNonNull(productId, "Product ID is required");

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     FIND_BY_ID_SQL)) {

            bindId(statement, productId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapRow(resultSet));
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to find stock by product ID", exception);
        }
    }

    @Override
    public List<Stock> findAll() {
        List<Stock> stocks = new ArrayList<>();

        try (
                Connection connection = connectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        FIND_ALL_SQL
                );
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                stocks.add(mapRow(resultSet));
            }

            return stocks;
        } catch (SQLException exception) {
            throw new DataAccessException(
                    "Failed to find all stocks",
                    exception
            );
        }
    }

    @Override
    public boolean update(Stock stock) {
        Objects.requireNonNull(stock, "Stock is required");

        try (
                Connection connection = connectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        UPDATE_SQL
                )
        ) {
            bindUpdate(statement, stock);

            int affectedRows = statement.executeUpdate();

            return affectedRows == 1;
        } catch (SQLException exception) {
            throw new DataAccessException(
                    "Failed to update stock",
                    exception
            );
        }
    }

    @Override
    public boolean deleteById(Long productId) {
        Objects.requireNonNull(productId, "Product ID is required");

        try (
                Connection connection = connectionFactory.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        DELETE_BY_ID_SQL
                )
        ) {
            bindId(statement, productId);

            int affectedRows = statement.executeUpdate();

            return affectedRows == 1;
        } catch (SQLException exception) {
            throw new DataAccessException(
                    "Failed to delete stock by product ID",
                    exception
            );
        }
    }

    private void bindInsert(PreparedStatement statement, Stock stock) throws SQLException {
        statement.setLong(1, stock.getProduct().getId());
        statement.setInt(2, stock.getAvailableQuantity());
    }

    private void bindId(PreparedStatement statement, Long productId) throws SQLException {
        statement.setLong(1, productId);
    }

    private void bindUpdate(PreparedStatement statement, Stock stock) throws SQLException {
        statement.setInt(1, stock.getAvailableQuantity());
        statement.setLong(2, stock.getProduct().getId());
    }

    private Stock mapRow(ResultSet resultSet) throws SQLException {
        Product product = new Product();
        product.setId(resultSet.getLong("product_id"));
        product.setName(resultSet.getString("name_product"));
        product.setCode(resultSet.getString("code_product"));
        product.setPrice(resultSet.getBigDecimal("price_product"));

        int availableQuantity = resultSet.getInt("available_quantity");

        return new Stock(product, availableQuantity);
    }
}
