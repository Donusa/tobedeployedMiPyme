package com.mipyme.tiendanube.dto;

public class ManualSyncRequest {
    private Long tnProductId;
    private Long localProductId;
    private java.util.Map<Long, Long> variantMapping;
    private boolean createNew;

    public ManualSyncRequest() {}

    public ManualSyncRequest(Long tnProductId, Long localProductId) {
        this.tnProductId = tnProductId;
        this.localProductId = localProductId;
    }

    public boolean isCreateNew() {
        return createNew;
    }

    public void setCreateNew(boolean createNew) {
        this.createNew = createNew;
    }

    public Long getTnProductId() {
        return tnProductId;
    }

    public void setTnProductId(Long tnProductId) {
        this.tnProductId = tnProductId;
    }

    public Long getLocalProductId() {
        return localProductId;
    }

    public void setLocalProductId(Long localProductId) {
        this.localProductId = localProductId;
    }

    public java.util.Map<Long, Long> getVariantMapping() {
        return variantMapping;
    }

    public void setVariantMapping(java.util.Map<Long, Long> variantMapping) {
        this.variantMapping = variantMapping;
    }
}
