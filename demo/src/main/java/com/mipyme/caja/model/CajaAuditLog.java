package com.mipyme.caja.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "caja_audit_log")
public class CajaAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "caja_diaria_id", nullable = false)
    private Long cajaDiariaId;

    @Column(name = "entidad", nullable = false, length = 50)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    @Column(name = "usuario", nullable = false)
    private String usuario;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "payload_anterior_json", columnDefinition = "TEXT")
    private String payloadAnteriorJson;

    @Column(name = "payload_nuevo_json", columnDefinition = "TEXT")
    private String payloadNuevoJson;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    protected CajaAuditLog() {
    }

    public CajaAuditLog(Long cajaDiariaId, String entidad, Long entidadId,
            String accion, String usuario, String payloadAnteriorJson,
            String payloadNuevoJson, String descripcion) {
        this.cajaDiariaId = cajaDiariaId;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.accion = accion;
        this.usuario = usuario;
        this.fechaHora = LocalDateTime.now();
        this.payloadAnteriorJson = payloadAnteriorJson;
        this.payloadNuevoJson = payloadNuevoJson;
        this.descripcion = descripcion;
    }



    public Long getAuditId() {
        return auditId;
    }

    public Long getCajaDiariaId() {
        return cajaDiariaId;
    }

    public String getEntidad() {
        return entidad;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public String getAccion() {
        return accion;
    }

    public String getUsuario() {
        return usuario;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getPayloadAnteriorJson() {
        return payloadAnteriorJson;
    }

    public String getPayloadNuevoJson() {
        return payloadNuevoJson;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
