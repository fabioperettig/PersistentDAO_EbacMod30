package com.fabioperettig.service;

import com.fabioperettig.dao.SaleDAO;
import com.fabioperettig.domain.Sale;

import java.util.Objects;
import java.util.Optional;

public class SaleService {

    private final SaleDAO saleDAO;

    public SaleService(SaleDAO saleDAO) {
        this.saleDAO = Objects.requireNonNull(
                saleDAO, "Sale DAO is required"
        );
    }

    public Sale create(Sale sale) {
        return saleDAO.create(sale);
    }

    public Optional<Sale> findById(Long saleId) {
        return saleDAO.findById(saleId);
    }

    public void complete(Long saleId) {
        saleDAO.complete(saleId);
    }

    public void cancel(Long saleId) {
        saleDAO.cancel(saleId);
    }
}
