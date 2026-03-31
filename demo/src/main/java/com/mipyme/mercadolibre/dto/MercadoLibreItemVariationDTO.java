package com.mipyme.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MercadoLibreItemVariationDTO {
    private Long id;
    private Double price;
    @JsonProperty("available_quantity")
    private Integer availableQuantity;
    @JsonProperty("attribute_combinations")
    private Object attributeCombinations;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Object getAttributeCombinations() {
        return attributeCombinations;
    }

    public void setAttributeCombinations(Object attributeCombinations) {
        this.attributeCombinations = attributeCombinations;
    }
}
