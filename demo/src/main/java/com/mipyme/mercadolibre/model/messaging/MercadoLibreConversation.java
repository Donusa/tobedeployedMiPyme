package com.mipyme.mercadolibre.model.messaging;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meli_conversations")
public class MercadoLibreConversation {
    @Id
    private Long id;

    private Long sellerId;
    private Long buyerId;

    private String status;
    private boolean blocked;
    private String substatus;

    private LocalDateTime lastMessageDate;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("dateCreated ASC")
    @JsonIgnore
    private List<MercadoLibreMessage> messages = new ArrayList<>();

    private int unreadCount;


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public String getSubstatus() { return substatus; }
    public void setSubstatus(String substatus) { this.substatus = substatus; }
    public LocalDateTime getLastMessageDate() { return lastMessageDate; }
    public void setLastMessageDate(LocalDateTime lastMessageDate) { this.lastMessageDate = lastMessageDate; }
    public List<MercadoLibreMessage> getMessages() { return messages; }
    public void setMessages(List<MercadoLibreMessage> messages) { this.messages = messages; }
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}
