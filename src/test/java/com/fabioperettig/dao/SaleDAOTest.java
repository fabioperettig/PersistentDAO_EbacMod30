package com.fabioperettig.dao;

import com.fabioperettig.domain.*;
import com.fabioperettig.exception.DataAccessException;
import com.fabioperettig.factory.ClientTestFactory;
import com.fabioperettig.factory.ProductTestFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SaleDAOTest extends DaoIntegrationTestSupport {

    private SaleDAO saleDAO;
    private ClientDAO clientDAO;
    private ProductDAO productDAO;
    private StockDAO stockDAO;

    @BeforeEach
    void setUp() {
        saleDAO = new SaleDAO(connectionFactory);
        clientDAO = new ClientDAO(connectionFactory);
        productDAO = new ProductDAO(connectionFactory);
        stockDAO = new StockDAO(connectionFactory);
    }

    @Test
    void shouldCreateAndFindSaleWithItems() {
        Client client = clientDAO.create(ClientTestFactory.create("12345678901"));

        Product product = ProductTestFactory.create("PROD0001");
        product.setPrice(new BigDecimal("25.50"));
        productDAO.create(product);

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 3);
        saleDAO.create(sale);

        Assertions.assertNotNull(sale.getId());

        Sale persistedSale = saleDAO.findById(sale.getId()).orElseThrow();

        Assertions.assertEquals(sale.getId(), persistedSale.getId());
        Assertions.assertEquals("SALE0001", persistedSale.getCode());
        Assertions.assertEquals(client.getId(), persistedSale.getClient().getId());
        Assertions.assertEquals(SaleStatus.INITIATED, persistedSale.getStatus());

        Assertions.assertEquals(1, persistedSale.getItems().size());

        SaleItem persistedItem = persistedSale.getItems().get(0);
        Assertions.assertEquals(product.getId(), persistedItem.getProduct().getId());
        Assertions.assertEquals(3, persistedItem.getQuantity());
        Assertions.assertEquals(new BigDecimal("25.50"), persistedItem.getUnitPrice());
        Assertions.assertEquals(new BigDecimal("76.50"), persistedSale.getTotal());
    }

    @Test
    void shouldPreserveItemPriceAfterProductPriceChanges() {
        Client client = clientDAO.create(ClientTestFactory.create("12345678901"));

        Product product = ProductTestFactory.create("PROD0002");
        product.setPrice(new BigDecimal("25.50"));
        productDAO.create(product);

        Sale sale = new Sale("SALE002", client);
        sale.addProduct(product, 3);
        saleDAO.create(sale);

        product.setPrice(new BigDecimal("30.00"));
        Assertions.assertTrue(productDAO.update(product));

        Sale persistedSale = saleDAO.findById(sale.getId()).orElseThrow();
        SaleItem persistedItem = persistedSale.getItems().get(0);

        Assertions.assertEquals(new BigDecimal("30.00"), persistedItem.getProduct().getPrice());
        Assertions.assertEquals(new BigDecimal("25.50"), persistedItem.getUnitPrice());
        Assertions.assertEquals(new BigDecimal("76.50"), persistedSale.getTotal());
    }

    @Test
    void shouldRollbackSaleWhenAnItemFails() throws SQLException {
        Client client = clientDAO.create(ClientTestFactory.create("12345678901"));

        Product validProduct = productDAO.create(ProductTestFactory.create("PROD0001"));

        Product missingProduct = ProductTestFactory.create("PROD0002");
        missingProduct.setId(999L);

        Assertions.assertTrue(productDAO.findById(999L).isEmpty());

        Sale sale = new Sale("SALE0003", client);
        sale.addProduct(validProduct, 2);
        sale.addProduct(missingProduct, 1);

        Assertions.assertThrows(
                DataAccessException.class,() -> saleDAO.create(sale)
        );

        Assertions.assertNull(sale.getId());

        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
             SELECT
                 (SELECT COUNT(*) FROM tb_sale) AS sale_count,
                 (SELECT COUNT(*) FROM tb_sale_item) AS item_count
             """)) {

            Assertions.assertTrue(resultSet.next());
            Assertions.assertEquals(0, resultSet.getInt("sale_count"));
            Assertions.assertEquals(0, resultSet.getInt("item_count"));
        }
    }

    @Test
    void shouldReturnEmptyWhenSaleDoesNotExist() {
        Assertions.assertTrue(saleDAO.findById(999L).isEmpty());
    }

    @Test
    void shouldCompleteSaleAndDecreaseStock() {
        Client client = clientDAO.create(ClientTestFactory.create("12345678901"));

        Product product = ProductTestFactory.create("PROD0003");
        product.setPrice(new BigDecimal("10.00"));
        productDAO.create(product);

        stockDAO.create(new Stock(product, 8));

        Sale sale = new Sale("SALE004", client);
        sale.addProduct(product, 3);
        saleDAO.create(sale);

        Assertions.assertEquals(
                8,stockDAO.findById(product.getId()).orElseThrow().getAvailableQuantity()
        );

        saleDAO.complete(sale.getId());

        Sale persistedSale = saleDAO.findById(sale.getId()).orElseThrow();
        Stock persistedStock = stockDAO.findById(product.getId()).orElseThrow();

        Assertions.assertEquals(SaleStatus.COMPLETED, persistedSale.getStatus());
        Assertions.assertEquals(3, persistedSale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("30.00"), persistedSale.getTotal());
        Assertions.assertEquals(5, persistedStock.getAvailableQuantity());
    }

    @Test
    void shouldRollbackCompletionWhenStockIsInsufficient() {
        Client client = clientDAO.create(ClientTestFactory.create("12345678901"));

        Product product1 = ProductTestFactory.create("PROD0004");
        Product product2 = ProductTestFactory.create("PROD0005");
        productDAO.create(product1);
        productDAO.create(product2);

        stockDAO.create(new Stock(product1, 8));
        stockDAO.create(new Stock(product2, 1));

        Sale sale = new Sale("SALE001", client);
        sale.addProduct(product1, 3);
        sale.addProduct(product2, 2);
        saleDAO.create(sale);

        Assertions.assertThrows(
                IllegalStateException.class,() -> saleDAO.complete(sale.getId())
        );

        Sale persistedSale = saleDAO.findById(sale.getId()).orElseThrow();
        Stock persistedStock1 = stockDAO.findById(product1.getId()).orElseThrow();
        Stock persistedStock2 = stockDAO.findById(product2.getId()).orElseThrow();

        Assertions.assertEquals(SaleStatus.INITIATED, persistedSale.getStatus());
        Assertions.assertEquals(8, persistedStock1.getAvailableQuantity());
        Assertions.assertEquals(1, persistedStock2.getAvailableQuantity());
    }

    @Test
    void shouldRejectCompletingSaleTwice() {
        Client client = clientDAO.create(ClientTestFactory.create("12345678901"));

        Product product = ProductTestFactory.create("PROD0001");
        productDAO.create(product);

        stockDAO.create(new Stock(product, 8));

        Sale sale = new Sale("SALE001", client);
        sale.addProduct(product, 3);
        saleDAO.create(sale);
        saleDAO.complete(sale.getId());

        Assertions.assertThrows(
                IllegalStateException.class, () -> saleDAO.complete(sale.getId())
        );

        Sale persistedSale = saleDAO.findById(sale.getId()).orElseThrow();
        Stock persistedStock = stockDAO.findById(product.getId()).orElseThrow();

        Assertions.assertEquals(SaleStatus.COMPLETED, persistedSale.getStatus());
        Assertions.assertEquals(5, persistedStock.getAvailableQuantity());
    }

    @Test
    void shouldCancelSaleWithoutChangingStock() {
        Client client = clientDAO.create(ClientTestFactory.create("12345678901"));

        Product product = ProductTestFactory.create("PROD0001");
        product.setPrice(new BigDecimal("10.00"));
        productDAO.create(product);

        stockDAO.create(new Stock(product, 8));

        Sale sale = new Sale("SALE001", client);
        sale.addProduct(product, 3);
        saleDAO.create(sale);
        saleDAO.cancel(sale.getId());

        Sale persistedSale = saleDAO.findById(sale.getId()).orElseThrow();
        Stock persistedStock = stockDAO.findById(product.getId()).orElseThrow();

        Assertions.assertEquals(SaleStatus.CANCELLED, persistedSale.getStatus());
        Assertions.assertEquals(1, persistedSale.getItems().size());
        Assertions.assertEquals(3, persistedSale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("30.00"), persistedSale.getTotal());
        Assertions.assertEquals(8, persistedStock.getAvailableQuantity());
    }

    @Test
    void shouldRejectCancellingCompletedSale() {
        Client client = clientDAO.create(ClientTestFactory.create("12345678901"));

        Product product = ProductTestFactory.create("PROD0001");
        product.setPrice(new BigDecimal("10.00"));
        productDAO.create(product);

        stockDAO.create(new Stock(product, 8));

        Sale sale = new Sale("SALE001", client);
        sale.addProduct(product, 3);
        saleDAO.create(sale);
        saleDAO.complete(sale.getId());

        Assertions.assertThrows(
                IllegalStateException.class, () -> saleDAO.cancel(sale.getId())
        );

        Sale persistedSale = saleDAO.findById(sale.getId()).orElseThrow();
        Stock persistedStock = stockDAO.findById(product.getId()).orElseThrow();

        Assertions.assertEquals(SaleStatus.COMPLETED, persistedSale.getStatus());
        Assertions.assertEquals(1, persistedSale.getItems().size());
        Assertions.assertEquals(3, persistedSale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("30.00"), persistedSale.getTotal());
        Assertions.assertEquals(5, persistedStock.getAvailableQuantity());
    }

    @Test
    void shouldRejectCompletingCancelledSale() {
        Client client = clientDAO.create(ClientTestFactory.create("12345678901"));

        Product product = ProductTestFactory.create("PROD0001");
        product.setPrice(new BigDecimal("10.00"));
        productDAO.create(product);

        stockDAO.create(new Stock(product, 8));

        Sale sale = new Sale("SALE001", client);
        sale.addProduct(product, 3);
        saleDAO.create(sale);
        saleDAO.cancel(sale.getId());

        Assertions.assertThrows(
                IllegalStateException.class, () -> saleDAO.complete(sale.getId())
        );

        Sale persistedSale = saleDAO.findById(sale.getId()).orElseThrow();
        Stock persistedStock = stockDAO.findById(product.getId()).orElseThrow();

        Assertions.assertEquals(SaleStatus.CANCELLED, persistedSale.getStatus());
        Assertions.assertEquals(1, persistedSale.getItems().size());
        Assertions.assertEquals(8, persistedStock.getAvailableQuantity());
    }
}
