package com.mipyme.caja.repository;

import com.mipyme.caja.model.CajaAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CajaAuditLogRepository extends JpaRepository<CajaAuditLog, Long> {
    List<CajaAuditLog> findByCajaDiariaIdOrderByFechaHoraDesc(Long cajaDiariaId);
}
