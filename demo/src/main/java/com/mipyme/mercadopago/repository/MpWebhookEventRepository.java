package com.mipyme.mercadopago.repository;

import com.mipyme.mercadopago.model.MpWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MpWebhookEventRepository extends JpaRepository<MpWebhookEvent, Long> {

    List<MpWebhookEvent> findByStatus(MpWebhookEvent.WebhookStatus status);

    List<MpWebhookEvent> findByStatusAndAttemptsLessThan(MpWebhookEvent.WebhookStatus status, int maxAttempts);
}
