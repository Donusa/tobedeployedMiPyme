package com.mipyme.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionChangeAuditRepository extends JpaRepository<SubscriptionChangeAudit, Long> {

    List<SubscriptionChangeAudit> findByTenantIdOrderByChangedAtDesc(String tenantId);


    Optional<SubscriptionChangeAudit> findTopByTenantIdOrderByChangedAtDesc(String tenantId);


    @Query("SELECT a FROM SubscriptionChangeAudit a WHERE a.tenantId = :tenantId AND a.prorationStatus = 'PENDING' ORDER BY a.changedAt DESC")
    Optional<SubscriptionChangeAudit> findPendingProration(@Param("tenantId") String tenantId);


    Optional<SubscriptionChangeAudit> findByProrationPaymentId(String prorationPaymentId);

    List<SubscriptionChangeAudit> findByTenantIdAndChangeTypeOrderByChangedAtDesc(String tenantId, String changeType);
}
