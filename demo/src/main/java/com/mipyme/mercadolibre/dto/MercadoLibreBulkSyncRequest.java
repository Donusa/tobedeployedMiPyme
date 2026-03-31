package com.mipyme.mercadolibre.dto;

import java.util.Map;

public class MercadoLibreBulkSyncRequest {
    private String mlItemId;
    private Long localProductId;
    private Boolean createNew;
    private Map<String, Long> variantMapping;

    public String getMlItemId() {
        return mlItemId;
    }

    public void setMlItemId(String mlItemId) {
        this.mlItemId = mlItemId;
    }

    public Long getLocalProductId() {
        return localProductId;
    }

    public void setLocalProductId(Long localProductId) {
        this.localProductId = localProductId;
    }

    public Boolean getCreateNew() {
        return createNew;
    }

    public void setCreateNew(Boolean createNew) {
        this.createNew = createNew;
    }

    public Map<String, Long> getVariantMapping() {
        return variantMapping;
    }

    public void setVariantMapping(Map<String, Long> variantMapping) {
        this.variantMapping = variantMapping;
    }
}
