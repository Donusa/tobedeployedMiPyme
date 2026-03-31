package com.mipyme.mercadolibre.dto;

import com.mipyme.stock.model.Product;
import com.mipyme.tiendanube.dto.SyncDifference;
import java.util.List;
import java.util.ArrayList;

public class MercadoLibreSyncPreviewDTO {
    private MercadoLibreItemDTO mercadoLibreItem;
    private Product localProduct;
    private boolean isNew;
    private List<SyncDifference> differences = new ArrayList<>();

    public MercadoLibreSyncPreviewDTO() {}

    public MercadoLibreSyncPreviewDTO(MercadoLibreItemDTO mercadoLibreItem, Product localProduct) {
        this.mercadoLibreItem = mercadoLibreItem;
        this.localProduct = localProduct;
        this.isNew = (localProduct == null);


        if (localProduct != null) {
            localProduct.setMeasurementUnit(null);
            localProduct.setProductCategory(null);
            localProduct.setProductBrand(null);
            localProduct.setWarehouse(null);
            localProduct.setStorage(null);
        }
    }

    public MercadoLibreItemDTO getMercadoLibreItem() {
        return mercadoLibreItem;
    }

    public void setMercadoLibreItem(MercadoLibreItemDTO mercadoLibreItem) {
        this.mercadoLibreItem = mercadoLibreItem;
    }

    public Product getLocalProduct() {
        return localProduct;
    }

    public void setLocalProduct(Product localProduct) {
        this.localProduct = localProduct;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public List<SyncDifference> getDifferences() {
        return differences;
    }

    public void setDifferences(List<SyncDifference> differences) {
        this.differences = differences;
    }

    public void addDifference(SyncDifference difference) {
        this.differences.add(difference);
    }
}
