package com.mipyme.caja.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CajaDiariaResponse {
    private Long id;
    private Long sucursalId;
    private String sucursalNombre;
    private LocalDate fechaOperativa;
    private String estado;
    private String usuarioApertura;
    private String usuarioResponsable;
    private String usuarioCierre;
    private LocalDateTime fechaHoraApertura;
    private LocalDateTime fechaHoraCierre;
    private BigDecimal montoAperturaEfectivo;
    private BigDecimal montoEsperadoEfectivo;
    private BigDecimal montoContadoEfectivo;
    private BigDecimal montoDiferencia;
    private String observacionApertura;
    private String observacionCierre;
    private boolean requiereRevision;



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSucursalId() {
        return sucursalId;
    }

    public void setSucursalId(Long sucursalId) {
        this.sucursalId = sucursalId;
    }

    public String getSucursalNombre() {
        return sucursalNombre;
    }

    public void setSucursalNombre(String sucursalNombre) {
        this.sucursalNombre = sucursalNombre;
    }

    public LocalDate getFechaOperativa() {
        return fechaOperativa;
    }

    public void setFechaOperativa(LocalDate fechaOperativa) {
        this.fechaOperativa = fechaOperativa;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getUsuarioApertura() {
        return usuarioApertura;
    }

    public void setUsuarioApertura(String usuarioApertura) {
        this.usuarioApertura = usuarioApertura;
    }

    public String getUsuarioResponsable() {
        return usuarioResponsable;
    }

    public void setUsuarioResponsable(String usuarioResponsable) {
        this.usuarioResponsable = usuarioResponsable;
    }

    public String getUsuarioCierre() {
        return usuarioCierre;
    }

    public void setUsuarioCierre(String usuarioCierre) {
        this.usuarioCierre = usuarioCierre;
    }

    public LocalDateTime getFechaHoraApertura() {
        return fechaHoraApertura;
    }

    public void setFechaHoraApertura(LocalDateTime fechaHoraApertura) {
        this.fechaHoraApertura = fechaHoraApertura;
    }

    public LocalDateTime getFechaHoraCierre() {
        return fechaHoraCierre;
    }

    public void setFechaHoraCierre(LocalDateTime fechaHoraCierre) {
        this.fechaHoraCierre = fechaHoraCierre;
    }

    public BigDecimal getMontoAperturaEfectivo() {
        return montoAperturaEfectivo;
    }

    public void setMontoAperturaEfectivo(BigDecimal montoAperturaEfectivo) {
        this.montoAperturaEfectivo = montoAperturaEfectivo;
    }

    public BigDecimal getMontoEsperadoEfectivo() {
        return montoEsperadoEfectivo;
    }

    public void setMontoEsperadoEfectivo(BigDecimal montoEsperadoEfectivo) {
        this.montoEsperadoEfectivo = montoEsperadoEfectivo;
    }

    public BigDecimal getMontoContadoEfectivo() {
        return montoContadoEfectivo;
    }

    public void setMontoContadoEfectivo(BigDecimal montoContadoEfectivo) {
        this.montoContadoEfectivo = montoContadoEfectivo;
    }

    public BigDecimal getMontoDiferencia() {
        return montoDiferencia;
    }

    public void setMontoDiferencia(BigDecimal montoDiferencia) {
        this.montoDiferencia = montoDiferencia;
    }

    public String getObservacionApertura() {
        return observacionApertura;
    }

    public void setObservacionApertura(String observacionApertura) {
        this.observacionApertura = observacionApertura;
    }

    public String getObservacionCierre() {
        return observacionCierre;
    }

    public void setObservacionCierre(String observacionCierre) {
        this.observacionCierre = observacionCierre;
    }

    public boolean isRequiereRevision() {
        return requiereRevision;
    }

    public void setRequiereRevision(boolean requiereRevision) {
        this.requiereRevision = requiereRevision;
    }
}
