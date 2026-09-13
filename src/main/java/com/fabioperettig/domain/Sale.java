package com.fabioperettig.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Sale {

    private Long id;
    private final String code;
    private final Client client;
    private final Map<Long, SaleItem> items;
    private final Instant saleDate;
    private SaleStatus status;

    public Sale(String code, Client client) {
        this(code, client, Instant.now());
    }

    private Sale(String code, Client client, Instant saleDate) {
        this.code = requireCode(code);
        this.client = requirePersistedClient(client);
        this.items = new LinkedHashMap<>();
        this.saleDate = Objects.requireNonNull(
                saleDate, "Sale date is required"
        );
        this.status = SaleStatus.INITIATED;
    }

    public void addProduct(Product product, int quantity) {
        validateModifiable();

        Long productId = requireProductId(product);
        SaleItem existingItem = items.get(productId);

        if (existingItem == null) {
            items.put(productId, new SaleItem(product, quantity));
        } else {
            existingItem.increaseQuantity(quantity);
        }
    }

    public void removeProduct(Product product, int quantity) {
        validateModifiable();

        Long productId = requireProductId(product);
        SaleItem existingItem = items.get(productId);

        if (existingItem == null) {
            throw new IllegalArgumentException("Product is not part of this sale");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (quantity > existingItem.getQuantity()) {
            throw new IllegalArgumentException("Quantity exceeds sale item quantity");
        }

        if (quantity == existingItem.getQuantity()) {
            items.remove(productId);
        } else {
            existingItem.decreaseQuantity(quantity);
        }
    }

    public int getTotalQuantity() {
        return items.values().stream()
                .mapToInt(SaleItem::getQuantity)
                .sum();
    }

    public BigDecimal getTotal() {
        return items.values().stream()
                .map(SaleItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void complete() {
        validateModifiable();

        if (items.isEmpty()) {
            throw new IllegalStateException("A sale without items cannot be completed");
        }

        status = SaleStatus.COMPLETED;
    }

    public void cancel() {
        validateModifiable();
        status = SaleStatus.CANCELLED;
    }

    private void validateModifiable() {
        if (!status.canBeModified()) {
            throw new IllegalStateException("Only initiated sales can be modified");
        }
    }

    private static String requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Sale code is required");
        }
        return code;
    }

    private static Client requirePersistedClient(Client client) {
        Objects.requireNonNull(client, "Client is required");

        if (client.getId() == null) {
            throw new IllegalArgumentException("Sale client must be persisted");
        }
        return client;
    }

    private static Long requireProductId(Product product) {
        Objects.requireNonNull(product, "Product is required");

        if (product.getId() == null) {
            throw new IllegalArgumentException("Sale product must be persisted");
        }
        return product.getId();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public Client getClient() {
        return client;
    }

    public List<SaleItem> getItems() {
        return List.copyOf(items.values());
    }

    public Instant getSaleDate() {
        return saleDate;
    }

    public SaleStatus getStatus() {
        return status;
    }

    /// recupera os dados do DB (chamado pelo DAO)
    public static Sale restore(
            Long id,
            String code,
            Client client,
            Instant saleDate,
            SaleStatus status,
            List<SaleItem> savedItems
    ) {
        Objects.requireNonNull(id, "Sale ID is required");
        Objects.requireNonNull(status, "Sale status is required");
        Objects.requireNonNull(savedItems, "Sale items are required");

        Sale sale = new Sale(code, client, saleDate);
        sale.id = id;

        for (SaleItem item : savedItems) {
            Objects.requireNonNull(item, "Sale item is required");
            Long productId = requireProductId(item.getProduct());

            SaleItem copy = new SaleItem(
                    item.getProduct(), item.getQuantity(), item.getUnitPrice()
            );

            if (sale.items.putIfAbsent(productId, copy) != null) {
                throw new IllegalArgumentException("Duplicate product in sale");
            }
        }

        if (status == SaleStatus.COMPLETED && sale.items.isEmpty()) {
            throw new IllegalArgumentException("Completed sale must have items");
        }

        sale.status = status;
        return sale;
    }
}
