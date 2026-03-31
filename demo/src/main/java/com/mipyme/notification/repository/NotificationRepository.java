package com.mipyme.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mipyme.notification.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByReferenceKey(String referenceKey);

    List<Notification> findByReadFalseOrderByCreatedAtDesc();

    long countByReadFalse();

    List<Notification> findAllByOrderByCreatedAtDesc();

    void deleteByReadTrueAndViewedAtBefore(LocalDateTime cutoff);
}
