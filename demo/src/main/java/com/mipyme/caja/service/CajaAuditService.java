package com.mipyme.caja.service;

import com.mipyme.caja.model.CajaAuditLog;
import com.mipyme.caja.repository.CajaAuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CajaAuditService {

    private final CajaAuditLogRepository auditLogRepository;

    public CajaAuditService(CajaAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void registrar(Long cajaDiariaId, String entidad, Long entidadId,
            String accion, String usuario,
            String payloadAnteriorJson, String payloadNuevoJson,
            String descripcion) {
        CajaAuditLog log = new CajaAuditLog(
                cajaDiariaId, entidad, entidadId, accion, usuario,
                payloadAnteriorJson, payloadNuevoJson, descripcion);
        auditLogRepository.save(log);
    }

    public List<CajaAuditLog> obtenerPorCaja(Long cajaDiariaId) {
        return auditLogRepository.findByCajaDiariaIdOrderByFechaHoraDesc(cajaDiariaId);
    }
}
