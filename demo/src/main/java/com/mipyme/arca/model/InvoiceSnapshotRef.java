package com.mipyme.arca.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_snapshot_ref", uniqueConstraints = {
        @UniqueConstraint(name = "uk_snapshot_invoice", columnNames = {"invoice_index_id"})
})
public class InvoiceSnapshotRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_index_id", nullable = false)
    private Long invoiceIndexId;

    @Column(name = "storage_type", nullable = false, length = 10)
    private String storageType;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "content_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String contentHash;

    @Column(name = "format", nullable = false, length = 10)
    private String format;

    @Column(name = "size_bytes")
    private Integer sizeBytes;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }



    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInvoiceIndexId() { return invoiceIndexId; }
    public void setInvoiceIndexId(Long invoiceIndexId) { this.invoiceIndexId = invoiceIndexId; }

    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Integer getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Integer sizeBytes) { this.sizeBytes = sizeBytes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }
}
