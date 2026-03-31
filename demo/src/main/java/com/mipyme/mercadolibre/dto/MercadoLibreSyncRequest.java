package com.mipyme.mercadolibre.dto;

import java.util.List;

public class MercadoLibreSyncRequest {
    private List<String> itemIds;

    public List<String> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<String> itemIds) {
        this.itemIds = itemIds;
    }
}
