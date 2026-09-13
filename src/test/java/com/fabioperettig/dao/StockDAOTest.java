package com.fabioperettig.dao;

import com.fabioperettig.domain.Product;
import com.fabioperettig.domain.Stock;
import com.fabioperettig.factory.ProductTestFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

}
