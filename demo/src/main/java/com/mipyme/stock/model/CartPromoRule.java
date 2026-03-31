package com.mipyme.stock.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_promo_rules")
public class CartPromoRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "tn_store_id", nullable = false)
    private Long tnStoreId;

    @Column(name = "tn_promotion_id")
    private String tnPromotionId;

    @Column(name = "rule_type", length = 20, nullable = false)
    private String ruleType;

    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    @Column(name = "pay_quantity")
    private Integer payQuantity;

    @Column(name = "target_variant_ids", columnDefinition = "TEXT")
    private String targetVariantIds;

    @Column(name = "target_category_ids", columnDefinition = "TEXT")
    private String targetCategoryIds;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CartPromoRule() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public Long getTnStoreId() {
        return tnStoreId;
    }

    public void setTnStoreId(Long tnStoreId) {
        this.tnStoreId = tnStoreId;
    }

    public String getTnPromotionId() {
        return tnPromotionId;
    }

    public void setTnPromotionId(String tnPromotionId) {
        this.tnPromotionId = tnPromotionId;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public Integer getBuyQuantity() {
        return buyQuantity;
    }

    public void setBuyQuantity(Integer buyQuantity) {
        this.buyQuantity = buyQuantity;
    }

    public Integer getPayQuantity() {
        return payQuantity;
    }

    public void setPayQuantity(Integer payQuantity) {
        this.payQuantity = payQuantity;
    }

    public String getTargetVariantIds() {
        return targetVariantIds;
    }

    public void setTargetVariantIds(String targetVariantIds) {
        this.targetVariantIds = targetVariantIds;
    }

    public String getTargetCategoryIds() {
        return targetCategoryIds;
    }

    public void setTargetCategoryIds(String targetCategoryIds) {
        this.targetCategoryIds = targetCategoryIds;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
