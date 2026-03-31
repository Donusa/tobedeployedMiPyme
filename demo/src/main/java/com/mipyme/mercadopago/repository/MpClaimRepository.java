package com.mipyme.mercadopago.repository;

import com.mipyme.mercadopago.model.MpClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface MpClaimRepository extends JpaRepository<MpClaim, Long> {

    Optional<MpClaim> findByDisputeId(String disputeId);

    List<MpClaim> findByTenantIdAndStatusNot(String tenantId, String status);
}
