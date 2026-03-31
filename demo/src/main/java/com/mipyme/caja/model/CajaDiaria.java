package com.mipyme.caja.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "caja_diaria")
public class CajaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "caja_diaria_id")
    private Long cajaDiariaId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "fecha_operativa", nullable = false)
    private LocalDate fechaOperativa;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private CajaEstado estado = CajaEstado.ABIERTA;

    @Column(name = "usuario_apertura", nullable = false)
    private String usuarioApertura;

    @Column(name = "usuario_responsable", nullable = false)
    private String usuarioResponsable;

    @Column(name = "usuario_cierre")
    private String usuarioCierre;

    @Column(name = "fecha_hora_apertura", nullable = false)
    private LocalDateTime fechaHoraApertura;

    @Column(name = "fecha_hora_cierre")
    private LocalDateTime fechaHoraCierre;

    @Column(name = "monto_apertura_efectivo", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoAperturaEfectivo;

    @Column(name = "monto_esperado_efectivo", precision = 19, scale = 2)
    private BigDecimal montoEsperadoEfectivo;

    @Column(name = "monto_contado_efectivo", precision = 19, scale = 2)
    private BigDecimal montoContadoEfectivo;

    @Column(name = "monto_diferencia", precision = 19, scale = 2)
    private BigDecimal montoDiferencia;

    @Column(name = "observacion_apertura", columnDefinition = "TEXT")
    private String observacionApertura;

    @Column(name = "observacion_cierre", columnDefinition = "TEXT")
    private String observacionCierre;

    @Column(name = "requiere_revision", nullable = false)
    private boolean requiereRevision = false;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "cajaDiaria", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<CajaMovimiento> movimientos = new ArrayList<>();



    public Long getCajaDiariaId() {
        return cajaDiariaId;
    }

    public void setCajaDiariaId(Long cajaDiariaId) {
        this.cajaDiariaId = cajaDiariaId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public LocalDate getFechaOperativa() {
        return fechaOperativa;
    }

    public void setFechaOperativa(LocalDate fechaOperativa) {
        this.fechaOperativa = fechaOperativa;
    }

    public CajaEstado getEstado() {
        return estado;
    }

    public void setEstado(CajaEstado estado) {
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<CajaMovimiento> getMovimientos() {
        return movimientos;
    }

    public void setMovimientos(List<CajaMovimiento> movimientos) {
        this.movimientos = movimientos;
    }

    public void addMovimiento(CajaMovimiento mov) {
        movimientos.add(mov);
        mov.setCajaDiaria(this);
    }
}
