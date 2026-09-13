package com.fabioperettig.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StockTest {

    @Test
    void shouldCreateStockForPersistedProduct() {
        Product product = persistedProduct();

        Stock stock = new Stock(product, 10);

        Assertions.assertEquals(product, stock.getProduct());
        Assertions.assertEquals(10, stock.getAvailableQuantity());
        Assertions.assertTrue(stock.isAvailable());
    }

    @Test
    void shouldDecreaseAvailableQuantity() {
        Stock stock = new Stock(persistedProduct(), 10);

        stock.decreaseQuantity(4);

        Assertions.assertEquals(6, stock.getAvailableQuantity());
    }

    @Test
    void shouldRejectInsufficientStock() {
        Stock stock = new Stock(persistedProduct(), 10);

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> stock.decreaseQuantity(11)
        );

        Assertions.assertEquals(10, stock.getAvailableQuantity());
    }

    @Test
    void shouldRejectProductWithoutId() {
        Product product = new Product();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new Stock(product, 10)
        );
    }

    private Product persistedProduct() {
        Product product = new Product();
        product.setId(1L);
        return product;
    }
}
