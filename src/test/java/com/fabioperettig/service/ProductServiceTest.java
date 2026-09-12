package com.fabioperettig.service;

import com.fabioperettig.dao.IGenericDAO;
import com.fabioperettig.domain.Product;
import com.fabioperettig.factory.ProductTestFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

public class ProductServiceTest {

    private FakeProductDAO productDAO;
    private ProductService service;

    @BeforeEach
    void setUp() {
        productDAO = new FakeProductDAO();
        service = new ProductService(productDAO);
    }

    @Test
    void shouldRejectNullProductDAO() {
        NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class, () -> new ProductService(null));

        Assertions.assertEquals("Product DAO is required", exception.getMessage());
    }

    @Test
    void shouldDelegateProductCreationToDAO() {
        Product product = ProductTestFactory.create("PROD0001");

        Product createdProduct = service.create(product);

        Assertions.assertSame(product, productDAO.receivedProduct);
        Assertions.assertSame(product, createdProduct);
    }

    @Test
    void shouldDelegateFindByIdToDAO() {
        Product expectedProduct = ProductTestFactory.create("PROD0002");
        productDAO.productToFind = expectedProduct;

        Optional<Product> result = service.findById(10L);

        Assertions.assertEquals(10L, productDAO.receivedId);
        Assertions.assertSame(expectedProduct, result.orElseThrow());
    }

    @Test
    void shouldReturnEmptyWhenProductIsNotFound() {
        Optional<Product> result = service.findById(99L);

        Assertions.assertEquals(99L, productDAO.receivedId);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void shouldDelegateFindAllToDAO() {
        Product firstProduct = ProductTestFactory.create("PROD0003");
        Product secondProduct = ProductTestFactory.create("PROD0004");
        Product thirdProduct = ProductTestFactory.create("PROD0005");

        List<Product> expectedProducts = List.of(firstProduct, secondProduct, thirdProduct);
        productDAO.productsToFind = expectedProducts;

        List<Product> result = service.findAll();

        Assertions.assertSame(expectedProducts, result);
    }

    @Test
    void shouldReturnTrueWhenProductIsUpdated() {
        Product product = ProductTestFactory.create("PROD0006");
        productDAO.updateResult = true;

        boolean result = service.update(product);

        Assertions.assertSame(product, productDAO.receivedProductForUpdate);
        Assertions.assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenProductIsNotUpdated() {
        Product product = ProductTestFactory.create("PROD0007");
        productDAO.updateResult = false;

        boolean result = service.update(product);

        Assertions.assertSame(product, productDAO.receivedProductForUpdate);
        Assertions.assertFalse(result);
    }

    @Test
    void shouldReturnTrueWhenProductIsDeleted() {
        productDAO.deleteResult = true;
        boolean result = service.deleteById(10L);

        Assertions.assertEquals(10L, productDAO.receivedIdForDeletion);
        Assertions.assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenProductIsNotDeleted() {
        productDAO.deleteResult = false;
        boolean result = service.deleteById(99L);

        Assertions.assertEquals(99L, productDAO.receivedIdForDeletion);
        Assertions.assertFalse(result);
    }

    private static class FakeProductDAO implements IGenericDAO<Product, Long> {

        private Product receivedProduct;
        private Product productToFind;
        private Product receivedProductForUpdate;
        private Long receivedId;
        private Long receivedIdForDeletion;
        private List<Product> productsToFind = List.of();
        private boolean updateResult;
        private boolean deleteResult;

        @Override
        public Product create(Product entity) {
            receivedProduct = entity;
            return entity;
        }

        @Override
        public Optional<Product> findById(Long id) {
            receivedId = id;
            return Optional.ofNullable(productToFind);
        }

        @Override
        public List<Product> findAll() {
            return productsToFind;
        }

        @Override
        public boolean update(Product entity) {
            receivedProductForUpdate = entity;
            return updateResult;
        }

        @Override
        public boolean deleteById(Long id) {
            receivedIdForDeletion = id;
            return deleteResult;
        }
    }

}
