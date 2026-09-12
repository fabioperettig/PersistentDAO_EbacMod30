package com.fabioperettig.domain;

import com.fabioperettig.factory.ClientTestFactory;
import com.fabioperettig.factory.ProductTestFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class SaleTest {

    @Test
    void shouldCreateInitiatedSaleWithoutItems() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Sale sale = new Sale("SALE0001", client);

        Assertions.assertEquals(SaleStatus.INITIATED, sale.getStatus());
        Assertions.assertTrue(sale.getItems().isEmpty());
        Assertions.assertEquals(0, sale.getTotalQuantity());
        Assertions.assertEquals(BigDecimal.ZERO, sale.getTotal());
        Assertions.assertSame(client, sale.getClient());
    }

    @Test
    void shouldAddProductAndCalculateTotal() {
        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("25.50"));

        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 3);

        Assertions.assertEquals(1, sale.getItems().size());
        Assertions.assertEquals(3, sale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("76.50"), sale.getTotal());
        Assertions.assertSame(product, sale.getItems().get(0).getProduct());
    }

    @Test
    void shouldMergeQuantitiesWhenAddingSameProduct() {
        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 3);
        sale.addProduct(product, 2);

        Assertions.assertEquals(1, sale.getItems().size());
        Assertions.assertEquals(5, sale.getItems().get(0).getQuantity());
        Assertions.assertEquals(5, sale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("50.00"), sale.getTotal());
    }

    @Test
    void shouldCalculateTotalsForDifferentProducts() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product firstProduct = ProductTestFactory.create("PROD0001");
        firstProduct.setId(10L);
        firstProduct.setPrice(new BigDecimal("10.00"));

        Product secondProduct = ProductTestFactory.create("PROD0002");
        secondProduct.setId(20L);
        secondProduct.setPrice(new BigDecimal("25.50"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(firstProduct, 2);
        sale.addProduct(secondProduct, 3);

        Assertions.assertEquals(2, sale.getItems().size());
        Assertions.assertEquals(5, sale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("96.50"), sale.getTotal());
    }

    @Test
    void shouldRemovePartOfProductQuantity() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 5);
        sale.removeProduct(product, 2);

        Assertions.assertEquals(1, sale.getItems().size());
        Assertions.assertEquals(3, sale.getItems().get(0).getQuantity());
        Assertions.assertEquals(3, sale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("30.00"), sale.getTotal());
    }

    @Test
    void shouldRemoveItemWhenRemovingAllUnits() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 5);
        sale.removeProduct(product, 5);

        Assertions.assertTrue(sale.getItems().isEmpty());
        Assertions.assertEquals(0, sale.getTotalQuantity());
        Assertions.assertEquals(BigDecimal.ZERO, sale.getTotal());
    }

    @Test
    void shouldRejectRemovingMoreThanExistingQuantity() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 5);

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> sale.removeProduct(product, 6)
        );

        Assertions.assertEquals(1, sale.getItems().size());
        Assertions.assertEquals(5, sale.getItems().get(0).getQuantity());
        Assertions.assertEquals(5, sale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("50.00"), sale.getTotal());
    }

    @Test
    void shouldRejectRemovingNegativeQuantity() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 5);

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> sale.removeProduct(product, -1)
        );

        Assertions.assertEquals(1, sale.getItems().size());
        Assertions.assertEquals(5, sale.getItems().get(0).getQuantity());
        Assertions.assertEquals(5, sale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("50.00"), sale.getTotal());
    }

    @Test
    void shouldRejectRemovingZeroQuantity() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 5);

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> sale.removeProduct(product, 0)
        );

        Assertions.assertEquals(1, sale.getItems().size());
        Assertions.assertEquals(5, sale.getItems().get(0).getQuantity());
        Assertions.assertEquals(5, sale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("50.00"), sale.getTotal());
    }

    @Test
    void shouldRejectRemovingProductNotInSale() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Product otherProduct = ProductTestFactory.create("PROD0002");
        otherProduct.setId(20L);
        otherProduct.setPrice(new BigDecimal("25.50"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 5);

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> sale.removeProduct(otherProduct, 1)
        );

        Assertions.assertEquals(1, sale.getItems().size());
        Assertions.assertSame(product, sale.getItems().get(0).getProduct());
        Assertions.assertEquals(5, sale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("50.00"), sale.getTotal());
    }

    @Test
    void shouldCompleteSaleWithItems() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 2);
        sale.complete();

        Assertions.assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        Assertions.assertEquals(1, sale.getItems().size());
        Assertions.assertEquals(2, sale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("20.00"), sale.getTotal());
    }

    @Test
    void shouldCancelSaleWithItems() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 2);
        sale.cancel();

        Assertions.assertEquals(SaleStatus.CANCELLED, sale.getStatus());
        Assertions.assertEquals(1, sale.getItems().size());
        Assertions.assertEquals(2, sale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("20.00"), sale.getTotal());
    }

    @Test
    void shouldRejectCompletingEmptySale() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Sale sale = new Sale("SALE0001", client);

        Assertions.assertThrows(
                IllegalStateException.class, () -> sale.complete()
        );

        Assertions.assertEquals(SaleStatus.INITIATED, sale.getStatus());
        Assertions.assertEquals(0, sale.getTotalQuantity());
        Assertions.assertTrue(sale.getItems().isEmpty());
    }

    @Test
    void shouldRejectAddingProductToCompletedSale() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 2);
        sale.complete();

        Assertions.assertThrows(
                IllegalStateException.class, () -> sale.addProduct(product, 1)
        );

        Assertions.assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        Assertions.assertEquals(1, sale.getItems().size());
        Assertions.assertEquals(2, sale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("20.00"), sale.getTotal());
    }

    @Test
    void shouldRejectRemovingProductFromCompletedSale() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 2);
        sale.complete();

        Assertions.assertThrows(
                IllegalStateException.class, () -> sale.removeProduct(product, 1)
        );

        Assertions.assertEquals(SaleStatus.COMPLETED, sale.getStatus());
        Assertions.assertEquals(1, sale.getItems().size());
        Assertions.assertEquals(2, sale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("20.00"), sale.getTotal());
    }

    @Test
    void shouldRejectAddingProductToCancelledSale() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 2);
        sale.cancel();

        Assertions.assertThrows(
                IllegalStateException.class, () -> sale.addProduct(product, 1)
        );

        Assertions.assertEquals(SaleStatus.CANCELLED, sale.getStatus());
        Assertions.assertEquals(1, sale.getItems().size());
        Assertions.assertEquals(2, sale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("20.00"), sale.getTotal());
    }

    @Test
    void shouldRejectRemovingProductFromCancelledSale() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 2);
        sale.cancel();

        Assertions.assertThrows(
                IllegalStateException.class, () -> sale.removeProduct(product, 1)
        );

        Assertions.assertEquals(SaleStatus.CANCELLED, sale.getStatus());
        Assertions.assertEquals(1, sale.getItems().size());
        Assertions.assertEquals(2, sale.getTotalQuantity());
        Assertions.assertEquals(new BigDecimal("20.00"), sale.getTotal());
    }

    @Test
    void shouldRejectCancellingCompletedSale() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 2);
        sale.complete();

        Assertions.assertThrows(
                IllegalStateException.class, () -> sale.cancel()
        );

        Assertions.assertEquals(SaleStatus.COMPLETED, sale.getStatus());
    }

    @Test
    void shouldRejectCompletingCancelledSale() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 2);
        sale.cancel();

        Assertions.assertThrows(
                IllegalStateException.class, () -> sale.complete()
        );

        Assertions.assertEquals(SaleStatus.CANCELLED, sale.getStatus());
    }

    @Test
    void shouldRejectCompletingAlreadyCompletedSale() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 2);
        sale.complete();

        Assertions.assertThrows(
                IllegalStateException.class, () -> sale.complete()
        );

        Assertions.assertEquals(SaleStatus.COMPLETED, sale.getStatus());
    }

    @Test
    void shouldRejectCancellingAlreadyCancelledSale() {
        Client client = ClientTestFactory.create("12345678901");
        client.setId(1L);

        Product product = ProductTestFactory.create("PROD0001");
        product.setId(10L);
        product.setPrice(new BigDecimal("10.00"));

        Sale sale = new Sale("SALE0001", client);
        sale.addProduct(product, 2);
        sale.cancel();

        Assertions.assertThrows(
                IllegalStateException.class, () -> sale.cancel()
        );

        Assertions.assertEquals(SaleStatus.CANCELLED, sale.getStatus());
    }

}
