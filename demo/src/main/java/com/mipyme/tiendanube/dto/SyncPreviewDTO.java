package com.mipyme.tiendanube.dto;

import com.mipyme.stock.model.Product;
import java.util.List;
import java.util.ArrayList;

public class SyncPreviewDTO {
    private TiendaNubeProduct tiendaNubeProduct;
    private Product localProduct;
    private boolean isNew;
    private List<SyncDifference> differences = new ArrayList<>();

    public SyncPreviewDTO() {}

    public SyncPreviewDTO(TiendaNubeProduct tiendaNubeProduct, Product localProduct) {
        this.tiendaNubeProduct = tiendaNubeProduct;
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

    public TiendaNubeProduct getTiendaNubeProduct() {
        return tiendaNubeProduct;
    }

    public void setTiendaNubeProduct(TiendaNubeProduct tiendaNubeProduct) {
        this.tiendaNubeProduct = tiendaNubeProduct;
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
