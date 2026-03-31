package com.mipyme.arca.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_audit_log", indexes = {
        @Index(name = "idx_audit_invoice_id", columnList = "invoice_index_id"),
        @Index(name = "idx_audit_created_at", columnList = "created_at")
})
public class InvoiceAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    @Column(name = "invoice_index_id")
    private Long invoiceIndexId;

    @Column(name = "fiscal_key", length = 80)
    private String fiscalKey;

    @Column(name = "actor", nullable = false, length = 50)
    private String actor;

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "request_hash", length = 64, columnDefinition = "CHAR(64)")
    private String requestHash;

    @Column(name = "response_hash", length = 64, columnDefinition = "CHAR(64)")
    private String responseHash;

    @Column(name = "prev_log_hash", length = 64, columnDefinition = "CHAR(64)")
    private String prevLogHash;

    @Column(name = "entry_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String entryHash;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }



    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getInvoiceIndexId() { return invoiceIndexId; }
    public void setInvoiceIndexId(Long invoiceIndexId) { this.invoiceIndexId = invoiceIndexId; }

    public String getFiscalKey() { return fiscalKey; }
    public void setFiscalKey(String fiscalKey) { this.fiscalKey = fiscalKey; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }

    public String getResponseHash() { return responseHash; }
    public void setResponseHash(String responseHash) { this.responseHash = responseHash; }

    public String getPrevLogHash() { return prevLogHash; }
    public void setPrevLogHash(String prevLogHash) { this.prevLogHash = prevLogHash; }

    public String getEntryHash() { return entryHash; }
    public void setEntryHash(String entryHash) { this.entryHash = entryHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
