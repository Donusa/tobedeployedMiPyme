package com.mipyme.caja.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "caja_movimiento",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_caja_mov_idempotency",
                columnNames = {"caja_diaria_id", "idempotency_key"}))
public class CajaMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movimiento_id")
    private Long movimientoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_diaria_id", nullable = false)
    @JsonBackReference
    private CajaDiaria cajaDiaria;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private CajaMovimientoTipo tipo;

    @Column(name = "categoria_egreso_id")
    private Long categoriaEgresoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false, length = 30)
    private MedioPago medioPago;

    @Column(name = "monto", nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "justificacion", columnDefinition = "TEXT")
    private String justificacion;

    @Column(name = "comprobante_texto", columnDefinition = "TEXT")
    private String comprobanteTexto;

    @Column(name = "comprobante_numero", length = 100)
    private String comprobanteNumero;

    @Column(name = "comprobante_archivo_url", length = 500)
    private String comprobanteArchivoUrl;

    @Column(name = "referencia_tipo", length = 50)
    private String referenciaTipo;

    @Column(name = "referencia_id", length = 100)
    private String referenciaId;

    @Column(name = "usuario_creador", nullable = false)
    private String usuarioCreador;

    @Column(name = "usuario_anulador")
    private String usuarioAnulador;

    @Column(name = "fecha_hora_creacion", nullable = false)
    private LocalDateTime fechaHoraCreacion;

    @Column(name = "fecha_hora_anulacion")
    private LocalDateTime fechaHoraAnulacion;

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private CajaMovimientoEstado estado = CajaMovimientoEstado.ACTIVO;

    @Column(name = "movimiento_origen_id")
    private Long movimientoOrigenId;

    @Column(name = "entidad_pago", length = 100)
    private String entidadPago;

    @Column(name = "referencia_externa_pago")
    private String referenciaExternaPago;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;



    public Long getMovimientoId() {
        return movimientoId;
    }

    public void setMovimientoId(Long movimientoId) {
        this.movimientoId = movimientoId;
    }

    public CajaDiaria getCajaDiaria() {
        return cajaDiaria;
    }

    public void setCajaDiaria(CajaDiaria cajaDiaria) {
        this.cajaDiaria = cajaDiaria;
    }

    public CajaMovimientoTipo getTipo() {
        return tipo;
    }

    public void setTipo(CajaMovimientoTipo tipo) {
        this.tipo = tipo;
    }

    public Long getCategoriaEgresoId() {
        return categoriaEgresoId;
    }

    public void setCategoriaEgresoId(Long categoriaEgresoId) {
        this.categoriaEgresoId = categoriaEgresoId;
    }

    public MedioPago getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(MedioPago medioPago) {
        this.medioPago = medioPago;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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

    public String getComprobanteArchivoUrl() {
        return comprobanteArchivoUrl;
    }

    public void setComprobanteArchivoUrl(String comprobanteArchivoUrl) {
        this.comprobanteArchivoUrl = comprobanteArchivoUrl;
    }

    public String getReferenciaTipo() {
        return referenciaTipo;
    }

    public void setReferenciaTipo(String referenciaTipo) {
        this.referenciaTipo = referenciaTipo;
    }

    public String getReferenciaId() {
        return referenciaId;
    }

    public void setReferenciaId(String referenciaId) {
        this.referenciaId = referenciaId;
    }

    public String getUsuarioCreador() {
        return usuarioCreador;
    }

    public void setUsuarioCreador(String usuarioCreador) {
        this.usuarioCreador = usuarioCreador;
    }

    public String getUsuarioAnulador() {
        return usuarioAnulador;
    }

    public void setUsuarioAnulador(String usuarioAnulador) {
        this.usuarioAnulador = usuarioAnulador;
    }

    public LocalDateTime getFechaHoraCreacion() {
        return fechaHoraCreacion;
    }

    public void setFechaHoraCreacion(LocalDateTime fechaHoraCreacion) {
        this.fechaHoraCreacion = fechaHoraCreacion;
    }

    public LocalDateTime getFechaHoraAnulacion() {
        return fechaHoraAnulacion;
    }

    public void setFechaHoraAnulacion(LocalDateTime fechaHoraAnulacion) {
        this.fechaHoraAnulacion = fechaHoraAnulacion;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public CajaMovimientoEstado getEstado() {
        return estado;
    }

    public void setEstado(CajaMovimientoEstado estado) {
        this.estado = estado;
    }

    public Long getMovimientoOrigenId() {
        return movimientoOrigenId;
    }

    public void setMovimientoOrigenId(Long movimientoOrigenId) {
        this.movimientoOrigenId = movimientoOrigenId;
    }

    public String getEntidadPago() {
        return entidadPago;
    }

    public void setEntidadPago(String entidadPago) {
        this.entidadPago = entidadPago;
    }

    public String getReferenciaExternaPago() {
        return referenciaExternaPago;
    }

    public void setReferenciaExternaPago(String referenciaExternaPago) {
        this.referenciaExternaPago = referenciaExternaPago;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
