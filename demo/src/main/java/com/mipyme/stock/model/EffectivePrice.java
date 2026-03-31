package com.mipyme.stock.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "effective_prices", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "local_product_id", "channel" })
})
public class EffectivePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "local_product_id", nullable = false)
    private Long localProductId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 10, nullable = false)
    private Channel channel;

    @Column(name = "effective_price", precision = 19, scale = 2, nullable = false)
    private BigDecimal effectivePrice;

    @Column(name = "regular_price", precision = 19, scale = 2, nullable = false)
    private BigDecimal regularPrice;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    @Column(name = "source_offer_id")
    private Long sourceOfferId;

    public EffectivePrice() {
        this.computedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLocalProductId() {
        return localProductId;
    }

    public void setLocalProductId(Long localProductId) {
        this.localProductId = localProductId;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public BigDecimal getEffectivePrice() {
        return effectivePrice;
    }

    public void setEffectivePrice(BigDecimal effectivePrice) {
        this.effectivePrice = effectivePrice;
    }

    public BigDecimal getRegularPrice() {
        return regularPrice;
    }

    public void setRegularPrice(BigDecimal regularPrice) {
        this.regularPrice = regularPrice;
    }

    public LocalDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(LocalDateTime computedAt) {
        this.computedAt = computedAt;
    }

    public Long getSourceOfferId() {
        return sourceOfferId;
    }

    public void setSourceOfferId(Long sourceOfferId) {
        this.sourceOfferId = sourceOfferId;
    }
}
