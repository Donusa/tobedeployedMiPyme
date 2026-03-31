package com.mipyme.stock.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_movement_id")
    private Long stockMovementId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "movement_type", nullable = false)
    private String movementType;

    @ManyToOne
    @JoinColumn(name = "product_variant_id", nullable = false)
    @JsonIgnoreProperties("attributes")
    private ProductVariant productVariant;

    @Column(name = "quantity_units", nullable = false)
    private BigDecimal quantityUnits;

    @ManyToOne
    @JoinColumn(name = "from_storage_location_id")
    private StorageLocation fromStorageLocation;

    @ManyToOne
    @JoinColumn(name = "to_storage_location_id")
    private StorageLocation toStorageLocation;

    @Column(name = "reference_source")
    private String referenceSource;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "note")
    private String note;

    protected StockMovement() {
    }

    public StockMovement(Instant occurredAt, String movementType, ProductVariant productVariant, BigDecimal quantityUnits) {
        this.occurredAt = occurredAt;
        this.movementType = movementType;
        this.productVariant = productVariant;
        this.quantityUnits = quantityUnits;
    }

    public Long getStockMovementId() {
        return stockMovementId;
    }

    public void setStockMovementId(Long stockMovementId) {
        this.stockMovementId = stockMovementId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public ProductVariant getProductVariant() {
        return productVariant;
    }

    public void setProductVariant(ProductVariant productVariant) {
        this.productVariant = productVariant;
    }

    public BigDecimal getQuantityUnits() {
        return quantityUnits;
    }

    public void setQuantityUnits(BigDecimal quantityUnits) {
        this.quantityUnits = quantityUnits;
    }

    public StorageLocation getFromStorageLocation() {
        return fromStorageLocation;
    }

    public void setFromStorageLocation(StorageLocation fromStorageLocation) {
        this.fromStorageLocation = fromStorageLocation;
    }

    public StorageLocation getToStorageLocation() {
        return toStorageLocation;
    }

    public void setToStorageLocation(StorageLocation toStorageLocation) {
        this.toStorageLocation = toStorageLocation;
    }

    public String getReferenceSource() {
        return referenceSource;
    }

    public void setReferenceSource(String referenceSource) {
        this.referenceSource = referenceSource;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
