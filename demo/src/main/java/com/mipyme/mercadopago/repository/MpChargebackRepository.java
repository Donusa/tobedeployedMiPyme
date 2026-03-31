package com.mipyme.mercadopago.repository;

import com.mipyme.mercadopago.model.MpChargeback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MpChargebackRepository extends JpaRepository<MpChargeback, Long> {

    Optional<MpChargeback> findByDisputeId(String disputeId);

    List<MpChargeback> findByTenantIdAndStatusNot(String tenantId, String status);
}
