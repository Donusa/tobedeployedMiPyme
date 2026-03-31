package com.mipyme.mercadopago.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mp_exchange_rate")
public class MpExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal rate;

    @Column(name = "last_plan_update_rate")
    private BigDecimal lastPlanUpdateRate;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(nullable = false)
    private String source;

    public MpExchangeRate() {}

    public MpExchangeRate(BigDecimal rate, String source) {
        this.rate = rate;
        this.source = source;
        this.fetchedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getLastPlanUpdateRate() { return lastPlanUpdateRate; }
    public void setLastPlanUpdateRate(BigDecimal lastPlanUpdateRate) { this.lastPlanUpdateRate = lastPlanUpdateRate; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime fetchedAt) { this.fetchedAt = fetchedAt; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
