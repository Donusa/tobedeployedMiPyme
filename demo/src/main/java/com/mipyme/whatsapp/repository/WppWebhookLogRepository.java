package com.mipyme.whatsapp.repository;

import com.mipyme.whatsapp.model.WppWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WppWebhookLogRepository extends JpaRepository<WppWebhookLog, Long> {

    boolean existsByEventHash(String eventHash);
}
