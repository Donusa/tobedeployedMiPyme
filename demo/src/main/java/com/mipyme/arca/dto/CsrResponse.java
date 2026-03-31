package com.mipyme.arca.dto;

public class CsrResponse {
    private String csr;
    private String privateKey;

    public CsrResponse(String csr, String privateKey) {
        this.csr = csr;
        this.privateKey = privateKey;
    }

    public String getCsr() {
        return csr;
    }

    public void setCsr(String csr) {
        this.csr = csr;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
