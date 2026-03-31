package com.mipyme.mercadopago.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mp_plan_config")
public class MpPlanConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_key", nullable = false, unique = true, length = 50)
    private String planKey;

    @Column(name = "mp_plan_id", nullable = false)
    private String mpPlanId;

    @Column(name = "price_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceUsd;

    @Column(name = "price_ars", precision = 19, scale = 2)
    private BigDecimal priceArs;

    @Column(nullable = false)
    private int frequency;

    @Column(name = "frequency_type", nullable = false, length = 20)
    private String frequencyType;

    @Column(length = 100)
    private String label;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public MpPlanConfig() {}

    public MpPlanConfig(String planKey, String mpPlanId, BigDecimal priceUsd, BigDecimal priceArs,
            int frequency, String frequencyType, String label) {
        this.planKey = planKey;
        this.mpPlanId = mpPlanId;
        this.priceUsd = priceUsd;
        this.priceArs = priceArs;
        this.frequency = frequency;
        this.frequencyType = frequencyType;
        this.label = label;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getPlanKey() { return planKey; }
    public void setPlanKey(String planKey) { this.planKey = planKey; }
    public String getMpPlanId() { return mpPlanId; }
    public void setMpPlanId(String mpPlanId) { this.mpPlanId = mpPlanId; }
    public BigDecimal getPriceUsd() { return priceUsd; }
    public void setPriceUsd(BigDecimal priceUsd) { this.priceUsd = priceUsd; }
    public BigDecimal getPriceArs() { return priceArs; }
    public void setPriceArs(BigDecimal priceArs) { this.priceArs = priceArs; }
    public int getFrequency() { return frequency; }
    public void setFrequency(int frequency) { this.frequency = frequency; }
    public String getFrequencyType() { return frequencyType; }
    public void setFrequencyType(String frequencyType) { this.frequencyType = frequencyType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
