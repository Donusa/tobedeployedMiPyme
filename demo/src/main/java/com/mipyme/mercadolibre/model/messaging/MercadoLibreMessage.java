package com.mipyme.mercadolibre.model.messaging;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meli_messages")
public class MercadoLibreMessage {
    @Id
    private String id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private MercadoLibreConversation conversation;

    @Column(name = "from_user_id")
    private Long fromUserId;

    @Column(name = "to_user_id")
    private Long toUserId;

    @Column(columnDefinition = "TEXT")
    private String text;

    private String status;

    private LocalDateTime dateCreated;
    private LocalDateTime dateRead;

    private boolean readByMe;


    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public MercadoLibreConversation getConversation() { return conversation; }
    public void setConversation(MercadoLibreConversation conversation) { this.conversation = conversation; }
    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }
    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }
    public LocalDateTime getDateRead() { return dateRead; }
    public void setDateRead(LocalDateTime dateRead) { this.dateRead = dateRead; }
    public boolean isReadByMe() { return readByMe; }
    public void setReadByMe(boolean readByMe) { this.readByMe = readByMe; }
}
