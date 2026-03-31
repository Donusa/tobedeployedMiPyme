package com.mipyme.mercadopago.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "mp_subscription")
public class MpSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false, unique = true)
    private String subscriptionId;

    @Column(nullable = false)
    private String status;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    private String period;

    @Column(name = "payer_email")
    private String payerEmail;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "plan_tier", length = 30)
    private String planTier;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();


    @Column(name = "next_payment_date")
    private Instant nextPaymentDate;


    @Column(name = "last_charged_date")
    private Instant lastChargedDate;


    @Column(name = "billing_day")
    private Integer billingDay;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPlanTier() {
        return planTier;
    }

    public void setPlanTier(String planTier) {
        this.planTier = planTier;
    }

    public Instant getNextPaymentDate() { return nextPaymentDate; }
    public void setNextPaymentDate(Instant nextPaymentDate) { this.nextPaymentDate = nextPaymentDate; }

    public Instant getLastChargedDate() { return lastChargedDate; }
    public void setLastChargedDate(Instant lastChargedDate) { this.lastChargedDate = lastChargedDate; }

    public Integer getBillingDay() { return billingDay; }
    public void setBillingDay(Integer billingDay) { this.billingDay = billingDay; }
}
