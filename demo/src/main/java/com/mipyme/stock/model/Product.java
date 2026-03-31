package com.mipyme.stock.model;

import java.util.ArrayList;
import java.util.List;

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
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "internal_code")
    private String internalCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @ManyToOne
    @JoinColumn(name = "measurement_unit_id")
    private MeasurementUnit measurementUnit;

    @Column(name = "measurement_value")
    private String measurementValue;

    @ManyToOne
    @JoinColumn(name = "product_category_id")
    private ProductCategory productCategory;

    @ManyToOne
    @JoinColumn(name = "product_brand_id")
    private ProductBrand productBrand;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "storage_location_id")
    private StorageLocation storage;

    @Column(name = "price")
    private java.math.BigDecimal price;

    @Column(name = "cost")
    private java.math.BigDecimal cost;

    @Column(name = "stock_quantity")
    private java.math.BigDecimal stockQuantity;

    @Column(name = "min_stock")
    private java.math.BigDecimal minStock;

    @Column(name = "tienda_nube_id")
    private Long tiendaNubeId;

    @Column(name = "mercadolibre_id")
    private String mercadoLibreId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> productVariants = new ArrayList<>();

    public Product() {
    }

    public Product(String internalCode, String productName, MeasurementUnit measurementUnit, ProductCategory productCategory, ProductBrand productBrand) {
        this.internalCode = internalCode;
        this.productName = productName;
        this.measurementUnit = measurementUnit;
        this.productCategory = productCategory;
        this.productBrand = productBrand;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getInternalCode() {
        return internalCode;
    }

    public void setInternalCode(String internalCode) {
        this.internalCode = internalCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public MeasurementUnit getMeasurementUnit() {
        return measurementUnit;
    }

    public void setMeasurementUnit(MeasurementUnit measurementUnit) {
        this.measurementUnit = measurementUnit;
    }

    public String getMeasurementValue() {
        return measurementValue;
    }

    public void setMeasurementValue(String measurementValue) {
        this.measurementValue = measurementValue;
    }

    public ProductCategory getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(ProductCategory productCategory) {
        this.productCategory = productCategory;
    }

    public ProductBrand getProductBrand() {
        return productBrand;
    }

    public void setProductBrand(ProductBrand productBrand) {
        this.productBrand = productBrand;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public StorageLocation getStorage() {
        return storage;
    }

    public void setStorage(StorageLocation storage) {
        this.storage = storage;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }

    public java.math.BigDecimal getCost() {
        return cost;
    }

    public void setCost(java.math.BigDecimal cost) {
        this.cost = cost;
    }

    public java.math.BigDecimal getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(java.math.BigDecimal stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public java.math.BigDecimal getMinStock() {
        return minStock;
    }

    public void setMinStock(java.math.BigDecimal minStock) {
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

    public List<ProductVariant> getProductVariants() {
        return productVariants;
    }

    public void setProductVariants(List<ProductVariant> productVariants) {
        this.productVariants.clear();
        if (productVariants != null) {
            productVariants.forEach(variant -> variant.setProduct(this));
            this.productVariants.addAll(productVariants);
        }
    }
}
