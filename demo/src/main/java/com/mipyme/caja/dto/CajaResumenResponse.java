package com.mipyme.caja.dto;

import java.math.BigDecimal;
import java.util.Map;

public class CajaResumenResponse {
    private Long cajaId;
    private String estado;
    private BigDecimal totalVendido;
    private BigDecimal totalCobrado;
    private long cantidadVentas;
    private long cantidadEgresos;
    private long cantidadDevoluciones;
    private long cantidadAnulaciones;
    private Map<String, BigDecimal> resumenPorMedioPago;
    private CajaFisicaResumen cajaFisica;

    public static class CajaFisicaResumen {
        private BigDecimal aperturaEfectivo;
        private BigDecimal ingresosEfectivo;
        private BigDecimal egresosEfectivo;
        private BigDecimal efectivoEsperado;
        private BigDecimal efectivoContado;
        private BigDecimal diferencia;

        public BigDecimal getAperturaEfectivo() {
            return aperturaEfectivo;
        }

        public void setAperturaEfectivo(BigDecimal aperturaEfectivo) {
            this.aperturaEfectivo = aperturaEfectivo;
        }

        public BigDecimal getIngresosEfectivo() {
            return ingresosEfectivo;
        }

        public void setIngresosEfectivo(BigDecimal ingresosEfectivo) {
            this.ingresosEfectivo = ingresosEfectivo;
        }

        public BigDecimal getEgresosEfectivo() {
            return egresosEfectivo;
        }

        public void setEgresosEfectivo(BigDecimal egresosEfectivo) {
            this.egresosEfectivo = egresosEfectivo;
        }

        public BigDecimal getEfectivoEsperado() {
            return efectivoEsperado;
        }

        public void setEfectivoEsperado(BigDecimal efectivoEsperado) {
            this.efectivoEsperado = efectivoEsperado;
        }

        public BigDecimal getEfectivoContado() {
            return efectivoContado;
        }

        public void setEfectivoContado(BigDecimal efectivoContado) {
            this.efectivoContado = efectivoContado;
        }

        public BigDecimal getDiferencia() {
            return diferencia;
        }

        public void setDiferencia(BigDecimal diferencia) {
            this.diferencia = diferencia;
        }
    }



    public Long getCajaId() {
        return cajaId;
    }

    public void setCajaId(Long cajaId) {
        this.cajaId = cajaId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getTotalVendido() {
        return totalVendido;
    }

    public void setTotalVendido(BigDecimal totalVendido) {
        this.totalVendido = totalVendido;
    }

    public BigDecimal getTotalCobrado() {
        return totalCobrado;
    }

    public void setTotalCobrado(BigDecimal totalCobrado) {
        this.totalCobrado = totalCobrado;
    }

    public long getCantidadVentas() {
        return cantidadVentas;
    }

    public void setCantidadVentas(long cantidadVentas) {
        this.cantidadVentas = cantidadVentas;
    }

    public long getCantidadEgresos() {
        return cantidadEgresos;
    }

    public void setCantidadEgresos(long cantidadEgresos) {
        this.cantidadEgresos = cantidadEgresos;
    }

    public long getCantidadDevoluciones() {
        return cantidadDevoluciones;
    }

    public void setCantidadDevoluciones(long cantidadDevoluciones) {
        this.cantidadDevoluciones = cantidadDevoluciones;
    }

    public long getCantidadAnulaciones() {
        return cantidadAnulaciones;
    }

    public void setCantidadAnulaciones(long cantidadAnulaciones) {
        this.cantidadAnulaciones = cantidadAnulaciones;
    }

    public Map<String, BigDecimal> getResumenPorMedioPago() {
        return resumenPorMedioPago;
    }

    public void setResumenPorMedioPago(Map<String, BigDecimal> resumenPorMedioPago) {
        this.resumenPorMedioPago = resumenPorMedioPago;
    }

    public CajaFisicaResumen getCajaFisica() {
        return cajaFisica;
    }

    public void setCajaFisica(CajaFisicaResumen cajaFisica) {
        this.cajaFisica = cajaFisica;
    }
}
