package com.fabioperettig.service;

import com.fabioperettig.dao.IGenericDAO;
import com.fabioperettig.domain.Stock;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StockService implements IGenericService<Stock, Long> {

    private final IGenericDAO<Stock, Long> stockDAO;

    public StockService(IGenericDAO<Stock, Long> stockDAO) {
        this.stockDAO = Objects.requireNonNull(stockDAO, "Stock DAO is required");
    }

    @Override
    public Stock create(Stock stock) {
        return stockDAO.create(stock);
    }

    @Override
    public Optional<Stock> findById(Long productId) {
        return stockDAO.findById(productId);
    }

    @Override
    public List<Stock> findAll() {
        return stockDAO.findAll();
    }

    @Override
    public boolean update(Stock stock) {
        return stockDAO.update(stock);
    }

    @Override
    public boolean deleteById(Long productId) {
        return stockDAO.deleteById(productId);
    }
}
