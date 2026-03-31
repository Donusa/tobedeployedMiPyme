package com.mipyme.whatsapp.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "wpp_messages")
public class WppMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wamid", nullable = false)
    private String wamid;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private WppConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 3)
    private MessageDirection direction;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "text_body", columnDefinition = "TEXT")
    private String textBody;

    @Column(name = "media_id")
    private String mediaId;

    @Column(name = "media_url_local")
    private String mediaUrlLocal;

    @Column(name = "media_mime_type")
    private String mediaMimeType;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    public enum MessageDirection {
        IN, OUT
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWamid() {
        return wamid;
    }

    public void setWamid(String wamid) {
        this.wamid = wamid;
    }

    public WppConversation getConversation() {
        return conversation;
    }

    public void setConversation(WppConversation conversation) {
        this.conversation = conversation;
    }

    public MessageDirection getDirection() {
        return direction;
    }

    public void setDirection(MessageDirection direction) {
        this.direction = direction;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTextBody() {
        return textBody;
    }

    public void setTextBody(String textBody) {
        this.textBody = textBody;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public String getMediaUrlLocal() {
        return mediaUrlLocal;
    }

    public void setMediaUrlLocal(String mediaUrlLocal) {
        this.mediaUrlLocal = mediaUrlLocal;
    }

    public String getMediaMimeType() {
        return mediaMimeType;
    }

    public void setMediaMimeType(String mediaMimeType) {
        this.mediaMimeType = mediaMimeType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }
}
