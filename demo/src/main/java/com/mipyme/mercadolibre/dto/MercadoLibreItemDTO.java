package com.mipyme.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MercadoLibreItemDTO {
    private String id;
    private String title;
    private Double price;
    @JsonProperty("currency_id")
    private String currencyId;
    @JsonProperty("available_quantity")
    private Integer availableQuantity;
    private String thumbnail;
    private String permalink;
    private java.util.List<MercadoLibreItemVariationDTO> variations;
    private String status;
    @JsonProperty("sub_status")
    private java.util.List<String> subStatus;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(String currencyId) {
        this.currencyId = currencyId;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getPermalink() {
        return permalink;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    public java.util.List<MercadoLibreItemVariationDTO> getVariations() {
        return variations;
    }

    public void setVariations(java.util.List<MercadoLibreItemVariationDTO> variations) {
        this.variations = variations;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.util.List<String> getSubStatus() {
        return subStatus;
    }

    public void setSubStatus(java.util.List<String> subStatus) {
        this.subStatus = subStatus;
    }
}
