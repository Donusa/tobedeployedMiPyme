package com.mipyme.caja.dto;

import java.math.BigDecimal;

public class CerrarCajaRequest {
    private BigDecimal montoContadoEfectivo;
    private String observacion;

    public BigDecimal getMontoContadoEfectivo() {
        return montoContadoEfectivo;
    }

    public void setMontoContadoEfectivo(BigDecimal montoContadoEfectivo) {
        this.montoContadoEfectivo = montoContadoEfectivo;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
