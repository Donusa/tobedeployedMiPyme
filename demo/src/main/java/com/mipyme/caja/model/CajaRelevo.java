package com.mipyme.caja.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "caja_relevo")
public class CajaRelevo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relevo_id")
    private Long relevoId;

    @Column(name = "caja_diaria_id", nullable = false)
    private Long cajaDiariaId;

    @Column(name = "usuario_saliente", nullable = false)
    private String usuarioSaliente;

    @Column(name = "usuario_entrante", nullable = false)
    private String usuarioEntrante;

    @Column(name = "fecha_hora_solicitud", nullable = false)
    private LocalDateTime fechaHoraSolicitud;

    @Column(name = "fecha_hora_confirmacion")
    private LocalDateTime fechaHoraConfirmacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private CajaRelevoEstado estado = CajaRelevoEstado.PENDIENTE;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "usuario_forzador")
    private String usuarioForzador;



    public Long getRelevoId() {
        return relevoId;
    }

    public void setRelevoId(Long relevoId) {
        this.relevoId = relevoId;
    }

    public Long getCajaDiariaId() {
        return cajaDiariaId;
    }

    public void setCajaDiariaId(Long cajaDiariaId) {
        this.cajaDiariaId = cajaDiariaId;
    }

    public String getUsuarioSaliente() {
        return usuarioSaliente;
    }

    public void setUsuarioSaliente(String usuarioSaliente) {
        this.usuarioSaliente = usuarioSaliente;
    }

    public String getUsuarioEntrante() {
        return usuarioEntrante;
    }

    public void setUsuarioEntrante(String usuarioEntrante) {
        this.usuarioEntrante = usuarioEntrante;
    }

    public LocalDateTime getFechaHoraSolicitud() {
        return fechaHoraSolicitud;
    }

    public void setFechaHoraSolicitud(LocalDateTime fechaHoraSolicitud) {
        this.fechaHoraSolicitud = fechaHoraSolicitud;
    }

    public LocalDateTime getFechaHoraConfirmacion() {
        return fechaHoraConfirmacion;
    }

    public void setFechaHoraConfirmacion(LocalDateTime fechaHoraConfirmacion) {
        this.fechaHoraConfirmacion = fechaHoraConfirmacion;
    }

    public CajaRelevoEstado getEstado() {
        return estado;
    }

    public void setEstado(CajaRelevoEstado estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getUsuarioForzador() {
        return usuarioForzador;
    }

    public void setUsuarioForzador(String usuarioForzador) {
        this.usuarioForzador = usuarioForzador;
    }
}
