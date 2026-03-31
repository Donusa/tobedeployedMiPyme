package com.mipyme.mercadopago.repository;

import com.mipyme.mercadopago.model.MpSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MpSubscriptionRepository extends JpaRepository<MpSubscription, Long> {

    Optional<MpSubscription> findBySubscriptionId(String subscriptionId);

    List<MpSubscription> findByTenantIdOrderByUpdatedAtDesc(String tenantId);
}
