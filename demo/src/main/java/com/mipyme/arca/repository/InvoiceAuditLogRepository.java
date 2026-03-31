package com.mipyme.arca.repository;

import com.mipyme.arca.model.InvoiceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceAuditLogRepository extends JpaRepository<InvoiceAuditLog, Long> {


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM InvoiceAuditLog a ORDER BY a.id DESC LIMIT 1")
    Optional<InvoiceAuditLog> findLastEntryForUpdate();

    List<InvoiceAuditLog> findByInvoiceIndexIdOrderByCreatedAtAsc(Long invoiceIndexId);

    List<InvoiceAuditLog> findAllByOrderByIdAsc();

    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
