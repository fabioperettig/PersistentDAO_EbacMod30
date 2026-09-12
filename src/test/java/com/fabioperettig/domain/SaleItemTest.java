package com.fabioperettig.domain;

import com.fabioperettig.factory.ProductTestFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class SaleItemTest {

    @Test
    void shouldCalculateSubTotal() {
        Product product = ProductTestFactory.create("PROD0001");
        product.setPrice(new BigDecimal("25.50"));

        SaleItem sale = new SaleItem(product,3);
        BigDecimal subTotal = sale.getSubtotal();

        Assertions.assertEquals(new BigDecimal("76.50"), subTotal);
    }

    @Test
    void shouldPreserveUnitPriceWhenProductPriceChanges() {
        Product product = ProductTestFactory.create("PROD0002");
        product.setPrice(new BigDecimal("25.50"));

        SaleItem sale = new SaleItem(product,3);
        product.setPrice(new BigDecimal("30.50"));

        Assertions.assertEquals(new BigDecimal("25.50"), sale.getUnitPrice());
        Assertions.assertEquals(new BigDecimal("76.50"), sale.getSubtotal());
    }

    @Test
    void shouldRejectZeroQuantity() {
        Product product = ProductTestFactory.create("PROD0003");

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> new SaleItem(product, 0)
        );
    }

    @Test
    void shouldRejectNegativeQuantity() {
        Product product = ProductTestFactory.create("PROD0004");


        Assertions.assertThrows(
                IllegalArgumentException.class, () -> new SaleItem(product, -1)
        );
    }

    @Test
    void shouldRejectNegativeUnitPrice() {
        Product product = ProductTestFactory.create("PROD0005");
        product.setPrice(new BigDecimal("-0.01"));

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> new SaleItem(product, 1)
        );
    }

    @Test
    void shouldAllowZeroUnitPrice() {
        Product product = ProductTestFactory.create("PROD0006");
        product.setPrice(new BigDecimal("0.00"));

        SaleItem sale = new SaleItem(product,2);

        Assertions.assertEquals( new BigDecimal("0.00"), sale.getUnitPrice());
        Assertions.assertEquals( new BigDecimal("0.00"), sale.getSubtotal());
    }

    @Test
    void shouldIncreaseQuantityAndRecalculateSubtotal() {
        Product product = ProductTestFactory.create("PROD0007");
        product.setPrice(new BigDecimal("10.00"));

        SaleItem sale = new SaleItem(product,2);
        sale.increaseQuantity(3);

        Assertions.assertEquals(5, sale.getQuantity());
        Assertions.assertEquals( new BigDecimal("50.00"), sale.getSubtotal());
    }

    @Test
    void shouldDecreaseQuantityAndRecalculateSubtotal() {
        Product product = ProductTestFactory.create("PROD0008");
        product.setPrice(new BigDecimal("10.00"));

        SaleItem sale = new SaleItem(product, 5);
        sale.decreaseQuantity(2);

        Assertions.assertEquals(3, sale.getQuantity());
        Assertions.assertEquals(new BigDecimal("30.00"), sale.getSubtotal());
    }

    @Test
    void shouldRejectDecreaseToZero() {
        Product product = ProductTestFactory.create("PROD0009");

        SaleItem sale = new SaleItem(product, 5);

        /// A operação deve falhar dentro da lambda.
        Assertions.assertThrows(
                IllegalArgumentException.class, () -> sale.decreaseQuantity(5)
        );

        Assertions.assertEquals(5, sale.getQuantity());
    }

    @Test
    void shouldRejectZeroIncrease() {
        Product product = ProductTestFactory.create("PROD0010");

        SaleItem sale = new SaleItem(product, 5);

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> sale.increaseQuantity(0)
        );

        Assertions.assertEquals(5, sale.getQuantity());
    }

    @Test
    void shouldRejectZeroDecrease() {
        Product product = ProductTestFactory.create("PROD0011");

        SaleItem sale = new SaleItem(product, 5);

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> sale.decreaseQuantity(0)
        );

        Assertions.assertEquals(5, sale.getQuantity());
    }

    @Test
    void shouldRejectNegativeIncrease() {
        Product product = ProductTestFactory.create("PROD0012");

        SaleItem sale = new SaleItem(product, 5);

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> sale.increaseQuantity(-1)
        );

        Assertions.assertEquals(5, sale.getQuantity());
    }

    @Test
    void shouldRejectNegativeDecrease() {
        Product product = ProductTestFactory.create("PROD0013");

        SaleItem sale = new SaleItem(product, 5);

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> sale.decreaseQuantity(-1)
        );

        Assertions.assertEquals(5, sale.getQuantity());
    }

    @Test
    void shouldRejectDecreaseBeyondCurrentQuantity() {
        Product product = ProductTestFactory.create("PROD0014");

        SaleItem sale = new SaleItem(product, 5);

        Assertions.assertThrows(
                IllegalArgumentException.class, () -> sale.decreaseQuantity(6)
        );

        Assertions.assertEquals(5, sale.getQuantity());
    }

    @Test
    void shouldRejectQuantityOverflow() {
        Product product = ProductTestFactory.create("PROD0015");

        SaleItem sale = new SaleItem(product, Integer.MAX_VALUE);

        Assertions.assertThrows(
                ArithmeticException.class, () -> sale.increaseQuantity(1)
        );

        Assertions.assertEquals(Integer.MAX_VALUE, sale.getQuantity());
    }

}
