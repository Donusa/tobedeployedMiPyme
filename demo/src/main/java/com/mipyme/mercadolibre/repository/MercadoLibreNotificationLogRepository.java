package com.mipyme.mercadolibre.repository;

import com.mipyme.mercadolibre.model.messaging.MercadoLibreNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MercadoLibreNotificationLogRepository extends JpaRepository<MercadoLibreNotificationLog, Long> {
    List<MercadoLibreNotificationLog> findByStatus(MercadoLibreNotificationLog.NotificationStatus status);
}
