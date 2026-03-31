package com.mipyme.notification;

import java.time.LocalDateTime;

import com.mipyme.notification.model.Notification.NotificationType;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        String url,
        String referenceKey,
        boolean read,
        LocalDateTime viewedAt,
        LocalDateTime createdAt) {
}
