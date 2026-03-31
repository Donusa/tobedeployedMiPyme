package com.mipyme.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mipyme.notification.model.Notification;
import com.mipyme.notification.model.Notification.NotificationType;
import com.mipyme.notification.repository.NotificationRepository;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }


    public Notification createIfNotExists(NotificationType type, String message, String url, String referenceKey) {
        try {
            if (repository.existsByReferenceKey(referenceKey)) {
                logger.debug("Notification already exists for referenceKey={}", referenceKey);
                return null;
            }
            Notification notification = new Notification(type, message, url, referenceKey);
            Notification saved = repository.save(notification);
            logger.info("Created notification: type={}, referenceKey={}, id={}", type, referenceKey, saved.getId());
            return saved;
        } catch (Exception e) {
            logger.error("Error creating notification: type={}, referenceKey={}, error={}", type, referenceKey,
                    e.getMessage(), e);
            return null;
        }
    }

    public List<NotificationResponse> getAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public long getUnreadCount() {
        return repository.countByReadFalse();
    }

    @Transactional
    public void markAsRead(Long id) {
        repository.findById(id).ifPresent(n -> {
            if (!n.isRead()) {
                n.setRead(true);
                n.setViewedAt(LocalDateTime.now());
                repository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead() {
        List<Notification> unread = repository.findByReadFalseOrderByCreatedAtDesc();
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : unread) {
            n.setRead(true);
            n.setViewedAt(now);
        }
        repository.saveAll(unread);
    }

    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        repository.deleteByReadTrueAndViewedAtBefore(cutoff);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getMessage(),
                n.getUrl(),
                n.getReferenceKey(),
                n.isRead(),
                n.getViewedAt(),
                n.getCreatedAt());
    }
}
