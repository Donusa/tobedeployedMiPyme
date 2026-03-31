package com.mipyme.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MercadoLibreItemResponseDTO {
    private Integer code;
    private MercadoLibreItemDTO body;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public MercadoLibreItemDTO getBody() {
        return body;
    }

    public void setBody(MercadoLibreItemDTO body) {
        this.body = body;
    }
}
