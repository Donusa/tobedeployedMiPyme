package com.mipyme.stock.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_balance")
public class StockBalance {

    @EmbeddedId
    private StockBalanceId id;

    @ManyToOne
    @MapsId("productVariantId")
    @JoinColumn(name = "product_variant_id")
    @JsonIgnoreProperties("attributes")
    private ProductVariant productVariant;

    @ManyToOne
    @MapsId("storageLocationId")
    @JoinColumn(name = "storage_location_id")
    private StorageLocation storageLocation;

    @Column(name = "quantity_on_hand")
    private BigDecimal quantityOnHand;

    @Column(name = "safety_stock_quantity")
    private BigDecimal safetyStockQuantity;

    @Column(name = "reorder_point_quantity")
    private BigDecimal reorderPointQuantity;

    @Column(name = "last_movement_at")
    private Instant lastMovementAt;

    protected StockBalance() {
    }

    public StockBalance(ProductVariant productVariant, StorageLocation storageLocation) {
        this.productVariant = productVariant;
        this.storageLocation = storageLocation;
        this.id = new StockBalanceId(productVariant.getProductVariantId(), storageLocation.getStorageLocationId());
    }

    public StockBalanceId getId() {
        return id;
    }

    public void setId(StockBalanceId id) {
        this.id = id;
    }

    public ProductVariant getProductVariant() {
        return productVariant;
    }

    public void setProductVariant(ProductVariant productVariant) {
        this.productVariant = productVariant;
        this.id.setProductVariantId(productVariant.getProductVariantId());
    }

    public StorageLocation getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(StorageLocation storageLocation) {
        this.storageLocation = storageLocation;
        this.id.setStorageLocationId(storageLocation.getStorageLocationId());
    }

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(BigDecimal quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public BigDecimal getSafetyStockQuantity() {
        return safetyStockQuantity;
    }

    public void setSafetyStockQuantity(BigDecimal safetyStockQuantity) {
        this.safetyStockQuantity = safetyStockQuantity;
    }

    public BigDecimal getReorderPointQuantity() {
        return reorderPointQuantity;
    }

    public void setReorderPointQuantity(BigDecimal reorderPointQuantity) {
        this.reorderPointQuantity = reorderPointQuantity;
    }

    public Instant getLastMovementAt() {
        return lastMovementAt;
    }

    public void setLastMovementAt(Instant lastMovementAt) {
        this.lastMovementAt = lastMovementAt;
    }
}
