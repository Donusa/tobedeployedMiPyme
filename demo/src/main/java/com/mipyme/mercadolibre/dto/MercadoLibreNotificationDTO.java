package com.mipyme.mercadolibre.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MercadoLibreNotificationDTO {
    @JsonProperty("_id")
    private String id;
    private String resource;
    @JsonProperty("user_id")
    private Long userId;
    private String topic;
    @JsonProperty("application_id")
    private Long applicationId;
    private Integer attempts;
    private String sent;
    private String received;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public String getSent() {
        return sent;
    }

    public void setSent(String sent) {
        this.sent = sent;
    }

    public String getReceived() {
        return received;
    }

    public void setReceived(String received) {
        this.received = received;
    }

    @Override
    public String toString() {
        return "MercadoLibreNotificationDTO{" +
                "id='" + id + '\'' +
                ", resource='" + resource + '\'' +
                ", userId=" + userId +
                ", topic='" + topic + '\'' +
                '}';
    }
}
