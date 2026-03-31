package com.mipyme.stock.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "offers")
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "offer_id")
    private Long offerId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "indefinite")
    private boolean indefinite;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private OfferTargetType targetType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "offer_target_ids", joinColumns = @JoinColumn(name = "offer_id"))
    @Column(name = "target_id")
    private List<Long> targetIds = new ArrayList<>();

    @Column(name = "discount_value")
    private BigDecimal discountValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType;

    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    @Column(name = "pay_quantity")
    private Integer payQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private OfferStatus status = OfferStatus.DRAFT;


    @Column(name = "ml_promotion_id")
    private String mlPromotionId;

    @Column(name = "ml_offer_id")
    private String mlOfferId;

    @Column(name = "ml_promotion_type")
    private String mlPromotionType;


    @Enumerated(EnumType.STRING)
    @Column(name = "tn_mode", length = 20)
    private TnMode tnMode;

    @Column(name = "tn_promotion_id")
    private String tnPromotionId;

    @Column(name = "published_tienda_nube")
    private Boolean publishedTiendaNube = false;

    @Column(name = "published_mercado_libre")
    private Boolean publishedMercadoLibre = false;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Transient
    private boolean hasTiendaNubeLinks;

    @Transient
    private boolean hasMercadoLibreLinks;

    @Transient
    private String tnGateReason;

    @Transient
    private String mlGateReason;

    public Offer() {
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isIndefinite() {
        return indefinite;
    }

    public void setIndefinite(boolean indefinite) {
        this.indefinite = indefinite;
    }

    public OfferTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(OfferTargetType targetType) {
        this.targetType = targetType;
    }

    public List<Long> getTargetIds() {
        return targetIds;
    }

    public void setTargetIds(List<Long> targetIds) {
        this.targetIds = targetIds;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
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

    public OfferStatus getStatus() {
        return status;
    }

    public void setStatus(OfferStatus status) {
        this.status = status;
    }

    public String getMlPromotionId() {
        return mlPromotionId;
    }

    public void setMlPromotionId(String mlPromotionId) {
        this.mlPromotionId = mlPromotionId;
    }

    public String getMlOfferId() {
        return mlOfferId;
    }

    public void setMlOfferId(String mlOfferId) {
        this.mlOfferId = mlOfferId;
    }

    public String getMlPromotionType() {
        return mlPromotionType;
    }

    public void setMlPromotionType(String mlPromotionType) {
        this.mlPromotionType = mlPromotionType;
    }

    public TnMode getTnMode() {
        return tnMode;
    }

    public void setTnMode(TnMode tnMode) {
        this.tnMode = tnMode;
    }

    public String getTnPromotionId() {
        return tnPromotionId;
    }

    public void setTnPromotionId(String tnPromotionId) {
        this.tnPromotionId = tnPromotionId;
    }

    public Boolean isPublishedTiendaNube() {
        return publishedTiendaNube;
    }

    public void setPublishedTiendaNube(Boolean publishedTiendaNube) {
        this.publishedTiendaNube = publishedTiendaNube;
    }

    public Boolean isPublishedMercadoLibre() {
        return publishedMercadoLibre;
    }

    public void setPublishedMercadoLibre(Boolean publishedMercadoLibre) {
        this.publishedMercadoLibre = publishedMercadoLibre;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isHasTiendaNubeLinks() {
        return hasTiendaNubeLinks;
    }

    public void setHasTiendaNubeLinks(boolean hasTiendaNubeLinks) {
        this.hasTiendaNubeLinks = hasTiendaNubeLinks;
    }

    public boolean isHasMercadoLibreLinks() {
        return hasMercadoLibreLinks;
    }

    public void setHasMercadoLibreLinks(boolean hasMercadoLibreLinks) {
        this.hasMercadoLibreLinks = hasMercadoLibreLinks;
    }

    public String getTnGateReason() {
        return tnGateReason;
    }

    public void setTnGateReason(String tnGateReason) {
        this.tnGateReason = tnGateReason;
    }

    public String getMlGateReason() {
        return mlGateReason;
    }

    public void setMlGateReason(String mlGateReason) {
        this.mlGateReason = mlGateReason;
    }
}
