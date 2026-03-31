package com.mipyme.mercadolibre.model.messaging;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meli_notification_log")
public class MercadoLibreNotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2000)
    private String payload;

    private String topic;
    private String resource;
    private Long userId;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;

    @Column(length = 1000)
    private String errorMessage;

    public enum NotificationStatus {
        PENDING, PROCESSED, ERROR
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
