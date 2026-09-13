package com.fabioperettig.service;

import com.fabioperettig.dao.IGenericDAO;
import com.fabioperettig.domain.Product;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ProductService implements IGenericService<Product, Long> {

    private final IGenericDAO<Product, Long> productDAO;

    public ProductService(IGenericDAO<Product, Long> productDAO) {
        this.productDAO = Objects.requireNonNull(productDAO, "Product DAO is required");
    }

    @Override
    public Product create(Product product) {
        return productDAO.create(product);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productDAO.findById(id);
    }

    @Override
    public List<Product> findAll() {
        return productDAO.findAll();
    }

    @Override
    public boolean update(Product product) {
        return productDAO.update(product);
    }

    @Override
    public boolean deleteById(Long id) {
        return productDAO.deleteById(id);
    }
}
