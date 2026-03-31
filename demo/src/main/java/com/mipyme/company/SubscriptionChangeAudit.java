package com.mipyme.company;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "subscription_change_audit", catalog = "mipyme")
public class SubscriptionChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;


    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();


    @Column(name = "change_type", nullable = false, length = 30)
    private String changeType;

    @Column(name = "from_plan_key", length = 30)
    private String fromPlanKey;

    @Column(name = "to_plan_key", length = 30)
    private String toPlanKey;


    @Column(name = "scheduled_effective_at")
    private Instant scheduledEffectiveAt;


    @Column(name = "effective_at")
    private Instant effectiveAt;


    @Column(name = "proration_amount", precision = 19, scale = 2)
    private BigDecimal prorationAmount;


    @Column(name = "proration_status", length = 20)
    private String prorationStatus = "NONE";


    @Column(name = "proration_payment_id", length = 100)
    private String prorationPaymentId;


    @Column(name = "proration_checkout_url", length = 512)
    private String prorationCheckoutUrl;


    @Column(name = "exchange_rate_used", precision = 19, scale = 4)
    private BigDecimal exchangeRateUsed;


    @Column(name = "initiated_by", nullable = false, length = 20)
    private String initiatedBy;

    @Column(name = "previous_status", length = 40)
    private String previousStatus;

    @Column(name = "new_status", length = 40)
    private String newStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    protected SubscriptionChangeAudit() {}



    public static SubscriptionChangeAudit of(String tenantId, String changeType, String initiatedBy) {
        SubscriptionChangeAudit a = new SubscriptionChangeAudit();
        a.tenantId = tenantId;
        a.changeType = changeType;
        a.initiatedBy = initiatedBy;
        a.changedAt = Instant.now();
        return a;
    }



    public Long getId() { return id; }

    public String getTenantId() { return tenantId; }

    public Instant getChangedAt() { return changedAt; }

    public String getChangeType() { return changeType; }

    public String getFromPlanKey() { return fromPlanKey; }
    public void setFromPlanKey(String fromPlanKey) { this.fromPlanKey = fromPlanKey; }

    public String getToPlanKey() { return toPlanKey; }
    public void setToPlanKey(String toPlanKey) { this.toPlanKey = toPlanKey; }

    public Instant getScheduledEffectiveAt() { return scheduledEffectiveAt; }
    public void setScheduledEffectiveAt(Instant scheduledEffectiveAt) { this.scheduledEffectiveAt = scheduledEffectiveAt; }

    public Instant getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(Instant effectiveAt) { this.effectiveAt = effectiveAt; }

    public BigDecimal getProrationAmount() { return prorationAmount; }
    public void setProrationAmount(BigDecimal prorationAmount) { this.prorationAmount = prorationAmount; }

    public String getProrationStatus() { return prorationStatus; }
    public void setProrationStatus(String prorationStatus) { this.prorationStatus = prorationStatus; }

    public String getProrationPaymentId() { return prorationPaymentId; }
    public void setProrationPaymentId(String prorationPaymentId) { this.prorationPaymentId = prorationPaymentId; }

    public String getProrationCheckoutUrl() { return prorationCheckoutUrl; }
    public void setProrationCheckoutUrl(String prorationCheckoutUrl) { this.prorationCheckoutUrl = prorationCheckoutUrl; }

    public BigDecimal getExchangeRateUsed() { return exchangeRateUsed; }
    public void setExchangeRateUsed(BigDecimal exchangeRateUsed) { this.exchangeRateUsed = exchangeRateUsed; }

    public String getInitiatedBy() { return initiatedBy; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
