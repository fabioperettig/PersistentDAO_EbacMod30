package com.fabioperettig.dao;

import com.fabioperettig.domain.Product;
import com.fabioperettig.domain.Stock;
import com.fabioperettig.exception.DataAccessException;
import com.fabioperettig.factory.ProductTestFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class StockDAOTest extends DaoIntegrationTestSupport {

    private StockDAO stockDAO;
    private ProductDAO productDAO;

    @BeforeEach
    void setUp() {
        stockDAO = new StockDAO(connectionFactory);
        productDAO = new ProductDAO(connectionFactory);
    }

    @Test
    void shouldCreateAndPersistStock() {
        Product product = productDAO.create(ProductTestFactory.create("STK00001"));
        stockDAO.create(new Stock(product, 8));

        Stock persistedStock = stockDAO.findById(product.getId()).orElseThrow();

        Assertions.assertEquals(8, persistedStock.getAvailableQuantity());
        Assertions.assertEquals(product.getId(), persistedStock.getProduct().getId());
        Assertions.assertEquals(product.getName(), persistedStock.getProduct().getName());
        Assertions.assertEquals(product.getCode(), persistedStock.getProduct().getCode());
        Assertions.assertEquals(product.getPrice(), persistedStock.getProduct().getPrice());

    }

    @Test
    void shouldUpdateStockQuantity() {
        Product product = productDAO.create(ProductTestFactory.create("STK00002"));
        Stock stock = new Stock(product, 8);
        stockDAO.create(stock);

        stock.decreaseQuantity(3);
        boolean updated = stockDAO.update(stock);

        Stock persistedStock = stockDAO.findById(product.getId()).orElseThrow();

        Assertions.assertTrue(updated);
        Assertions.assertEquals(5, persistedStock.getAvailableQuantity());
        Assertions.assertEquals(product.getId(), persistedStock.getProduct().getId());
    }

    @Test
    void shouldDeleteStockWithoutDeletingProduct() {
        Product product = productDAO.create(ProductTestFactory.create("STK00003"));
        Stock stock = new Stock(product, 8);
        stockDAO.create(stock);

        boolean deleted = stockDAO.deleteById(product.getId());

        Assertions.assertTrue(deleted);
        Assertions.assertTrue(stockDAO.findById(product.getId()).isEmpty());
        Assertions.assertTrue(productDAO.findById(product.getId()).isPresent());
    }

    @Test
    void shouldDeleteStockWhenProductIsDeleted() throws SQLException {
        Product product = productDAO.create(ProductTestFactory.create("STK00004"));
        Stock stock = new Stock(product, 8);
        stockDAO.create(stock);

        boolean deleted = productDAO.deleteById(product.getId());

        Assertions.assertTrue(deleted);
        Assertions.assertTrue(productDAO.findById(product.getId()).isEmpty());

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM tb_stock WHERE product_id = ?"
             )) {

            statement.setLong(1, product.getId());

            try (ResultSet resultSet = statement.executeQuery()) {
                Assertions.assertTrue(resultSet.next());
                Assertions.assertEquals(0, resultSet.getInt(1));
            }
        }
    }

    @Test
    void shouldFindAllStocks() {
        Product product1 = productDAO.create(ProductTestFactory.create("STK00005"));
        Product product2 = productDAO.create(ProductTestFactory.create("STK00006"));
        productDAO.create(ProductTestFactory.create("STK00007"));

        Stock stock1 = new Stock(product1, 8);
        Stock stock2 = new Stock(product2, 0);

        stockDAO.create(stock1);
        stockDAO.create(stock2);

        List<Stock> stocks = stockDAO.findAll();

        Assertions.assertEquals(2, stocks.size());

        Assertions.assertEquals(product1.getId(), stocks.get(0).getProduct().getId());
        Assertions.assertEquals(8, stocks.get(0).getAvailableQuantity());

        Assertions.assertEquals(product2.getId(), stocks.get(1).getProduct().getId());
        Assertions.assertEquals(0, stocks.get(1).getAvailableQuantity());
    }

    @Test
    void shouldRejectDuplicateStockForSameProduct() {
        Product product = productDAO.create(ProductTestFactory.create("STK00008"));
        Stock stock = new Stock(product, 8);
        stockDAO.create(stock);

        Assertions.assertThrows(
                DataAccessException.class, () -> stockDAO.create(new Stock(product, 3))
        );

        Stock persistedStock = stockDAO.findById(product.getId()).orElseThrow();
        Assertions.assertEquals(8, persistedStock.getAvailableQuantity());
    }

    @Test
    void shouldRejectStockForNonexistentProduct() {
        Product product = ProductTestFactory.create("STK00009");
        product.setId(999L);

        Assertions.assertTrue(productDAO.findById(product.getId()).isEmpty());

        Stock stock = new Stock(product, 8);

        Assertions.assertThrows(
                DataAccessException.class, () -> stockDAO.create(stock)
        );
    }

}
