package com.mipyme.stock.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_variant_attributes")
public class ProductVariantAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attribute_id")
    private Long attributeId;

    @ManyToOne
    @JoinColumn(name = "product_variant_id", nullable = false)
    @JsonBackReference
    private ProductVariant productVariant;

    @Column(name = "attribute_key", nullable = false)
    private String attributeKey;

    @Column(name = "attribute_value", nullable = false)
    private String attributeValue;

    public ProductVariantAttribute() {
    }

    public ProductVariantAttribute(ProductVariant productVariant, String attributeKey, String attributeValue) {
        this.productVariant = productVariant;
        this.attributeKey = attributeKey;
        this.attributeValue = attributeValue;
    }

    public Long getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(Long attributeId) {
        this.attributeId = attributeId;
    }

    public ProductVariant getProductVariant() {
        return productVariant;
    }

    public void setProductVariant(ProductVariant productVariant) {
        this.productVariant = productVariant;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductVariantAttribute that = (ProductVariantAttribute) o;
        return java.util.Objects.equals(attributeKey, that.attributeKey) &&
               java.util.Objects.equals(attributeValue, that.attributeValue);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(attributeKey, attributeValue);
    }
}
