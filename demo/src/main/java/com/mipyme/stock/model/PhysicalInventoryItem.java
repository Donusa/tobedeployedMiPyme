package com.mipyme.stock.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "physical_inventory_items")
public class PhysicalInventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "physical_inventory_item_id")
    private Long physicalInventoryItemId;

    @ManyToOne
    @JoinColumn(name = "physical_inventory_id", nullable = false)
    private PhysicalInventory physicalInventory;

    @ManyToOne
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @ManyToOne
    @JoinColumn(name = "storage_location_id")
    private StorageLocation storageLocation;

    @Column(name = "counted_quantity")
    private BigDecimal countedQuantity;

    protected PhysicalInventoryItem() {
    }

    public PhysicalInventoryItem(PhysicalInventory physicalInventory, ProductVariant productVariant, BigDecimal countedQuantity) {
        this.physicalInventory = physicalInventory;
        this.productVariant = productVariant;
        this.countedQuantity = countedQuantity;
    }

    public Long getPhysicalInventoryItemId() {
        return physicalInventoryItemId;
    }

    public void setPhysicalInventoryItemId(Long physicalInventoryItemId) {
        this.physicalInventoryItemId = physicalInventoryItemId;
    }

    public PhysicalInventory getPhysicalInventory() {
        return physicalInventory;
    }

    public void setPhysicalInventory(PhysicalInventory physicalInventory) {
        this.physicalInventory = physicalInventory;
    }

    public ProductVariant getProductVariant() {
        return productVariant;
    }

    public void setProductVariant(ProductVariant productVariant) {
        this.productVariant = productVariant;
    }

    public StorageLocation getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(StorageLocation storageLocation) {
        this.storageLocation = storageLocation;
    }

    public BigDecimal getCountedQuantity() {
        return countedQuantity;
    }

    public void setCountedQuantity(BigDecimal countedQuantity) {
        this.countedQuantity = countedQuantity;
    }
}
