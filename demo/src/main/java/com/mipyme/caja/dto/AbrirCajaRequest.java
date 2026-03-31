package com.mipyme.caja.dto;

import java.math.BigDecimal;

public class AbrirCajaRequest {
    private Long sucursalId;
    private BigDecimal montoAperturaEfectivo;
    private String observacion;

    public Long getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(Long sucursalId) {
        this.sucursalId = sucursalId;
    }

    public BigDecimal getMontoAperturaEfectivo() {
        return montoAperturaEfectivo;
    }

    public void setMontoAperturaEfectivo(BigDecimal montoAperturaEfectivo) {
        this.montoAperturaEfectivo = montoAperturaEfectivo;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
