package com.mipyme.stock.model;

import java.math.BigDecimal;
import java.util.Set;
import java.util.HashSet;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_variant_id")
    private Long productVariantId;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Product product;

    @Column(name = "variant_sku")
    private String variantSku;

    @Column(name = "variant_gtin")
    private String variantGtin;

    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<ProductVariantAttribute> attributes = new HashSet<>();

    @Column(name = "net_weight_grams")
    private BigDecimal netWeightGrams;

    @Column(name = "size_dimensions_cm")
    private String sizeDimensionsCm;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "cost")
    private BigDecimal cost;

    @Column(name = "stock_quantity")
    private BigDecimal stockQuantity;

    @Column(name = "min_stock")
    private BigDecimal minStock;

    @Column(name = "tienda_nube_id")
    private Long tiendaNubeId;

    @Column(name = "mercadolibre_id")
    private String mercadoLibreId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public ProductVariant() {
    }

    public ProductVariant(Product product, String variantSku, String variantGtin) {
        this.product = product;
        this.variantSku = variantSku;
        this.variantGtin = variantGtin;
    }

    public Long getProductVariantId() {
        return productVariantId;
    }

    public void setProductVariantId(Long productVariantId) {
        this.productVariantId = productVariantId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getVariantSku() {
        return variantSku;
    }

    public void setVariantSku(String variantSku) {
        this.variantSku = variantSku;
    }

    public String getVariantGtin() {
        return variantGtin;
    }

    public void setVariantGtin(String variantGtin) {
        this.variantGtin = variantGtin;
    }

    public Set<ProductVariantAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<ProductVariantAttribute> attributes) {
        this.attributes.clear();
        if (attributes != null) {
            attributes.forEach(attr -> attr.setProductVariant(this));
            this.attributes.addAll(attributes);
        }
    }

    public BigDecimal getNetWeightGrams() {
        return netWeightGrams;
    }

    public void setNetWeightGrams(BigDecimal netWeightGrams) {
        this.netWeightGrams = netWeightGrams;
    }

    public String getSizeDimensionsCm() {
        return sizeDimensionsCm;
    }

    public void setSizeDimensionsCm(String sizeDimensionsCm) {
        this.sizeDimensionsCm = sizeDimensionsCm;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(BigDecimal stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public BigDecimal getMinStock() {
        return minStock;
    }

    public void setMinStock(BigDecimal minStock) {
        this.minStock = minStock;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Long getTiendaNubeId() {
        return tiendaNubeId;
    }

    public void setTiendaNubeId(Long tiendaNubeId) {
        this.tiendaNubeId = tiendaNubeId;
    }

    public String getMercadoLibreId() {
        return mercadoLibreId;
    }

    public void setMercadoLibreId(String mercadoLibreId) {
        this.mercadoLibreId = mercadoLibreId;
    }
}
