package com.mipyme.mercadopago.repository;

import com.mipyme.mercadopago.model.MpPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MpPaymentRepository extends JpaRepository<MpPayment, Long> {

    Optional<MpPayment> findByPaymentId(String paymentId);

    List<MpPayment> findByTenantId(String tenantId);

    List<MpPayment> findByTenantIdOrderByApprovedAtDesc(String tenantId);

    long countByTenantIdAndStatus(String tenantId, String status);
}
