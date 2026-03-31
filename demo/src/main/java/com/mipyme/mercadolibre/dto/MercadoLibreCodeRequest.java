package com.mipyme.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MercadoLibreCodeRequest {
    private String code;

    @JsonProperty("code_verifier")
    private String codeVerifier;

    @JsonProperty("redirect_uri")
    private String redirectUri;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public void setCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
