package com.mipyme.caja.dto;

import java.math.BigDecimal;

public class RegistrarEgresoRequest {
    private Long categoriaId;
    private BigDecimal monto;
    private String medioPago;
    private String justificacion;
    private String comprobanteTexto;
    private String comprobanteNumero;
    private String observacion;
    private String tipo;

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(String medioPago) {
        this.medioPago = medioPago;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public void setJustificacion(String justificacion) {
        this.justificacion = justificacion;
    }

    public String getComprobanteTexto() {
        return comprobanteTexto;
    }

    public void setComprobanteTexto(String comprobanteTexto) {
        this.comprobanteTexto = comprobanteTexto;
    }

    public String getComprobanteNumero() {
        return comprobanteNumero;
    }

    public void setComprobanteNumero(String comprobanteNumero) {
        this.comprobanteNumero = comprobanteNumero;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
