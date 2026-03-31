package com.mipyme.stock.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class StockBalanceId implements Serializable {

    private static final long serialVersionUID = -3125549763086031051L;

	@Column(name = "product_variant_id")
    private Long productVariantId;

    @Column(name = "storage_location_id")
    private Long storageLocationId;

    public StockBalanceId() {
    }

    public StockBalanceId(Long productVariantId, Long storageLocationId) {
        this.productVariantId = productVariantId;
        this.storageLocationId = storageLocationId;
    }

    public Long getProductVariantId() {
        return productVariantId;
    }

    public void setProductVariantId(Long productVariantId) {
        this.productVariantId = productVariantId;
    }

    public Long getStorageLocationId() {
        return storageLocationId;
    }

    public void setStorageLocationId(Long storageLocationId) {
        this.storageLocationId = storageLocationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockBalanceId that = (StockBalanceId) o;
        return Objects.equals(productVariantId, that.productVariantId) && Objects.equals(storageLocationId, that.storageLocationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productVariantId, storageLocationId);
    }
}
