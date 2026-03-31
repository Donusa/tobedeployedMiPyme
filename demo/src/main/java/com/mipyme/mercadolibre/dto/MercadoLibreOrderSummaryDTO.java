package com.mipyme.mercadolibre.dto;

import java.math.BigDecimal;
import java.util.List;

public class MercadoLibreOrderSummaryDTO {
    private Long orderId;
    private Long packId;
    private Long buyerId;
    private String dateCreated;
    private String buyerNickname;
    private String buyerEmail;
    private BigDecimal totalAmount;
    private String currencyId;
    private String orderStatus;
    private Long shippingId;
    private String shippingStatus;
    private String shippingSubstatus;
    private String claimType;
    private String claimStatus;
    private List<MercadoLibreOrderItemDTO> items;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getPackId() {
        return packId;
    }

    public void setPackId(Long packId) {
        this.packId = packId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getBuyerNickname() {
        return buyerNickname;
    }

    public void setBuyerNickname(String buyerNickname) {
        this.buyerNickname = buyerNickname;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(String currencyId) {
        this.currencyId = currencyId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Long getShippingId() {
        return shippingId;
    }

    public void setShippingId(Long shippingId) {
        this.shippingId = shippingId;
    }

    public String getShippingStatus() {
        return shippingStatus;
    }

    public void setShippingStatus(String shippingStatus) {
        this.shippingStatus = shippingStatus;
    }

    public String getShippingSubstatus() {
        return shippingSubstatus;
    }

    public void setShippingSubstatus(String shippingSubstatus) {
        this.shippingSubstatus = shippingSubstatus;
    }

    public String getClaimType() {
        return claimType;
    }

    public void setClaimType(String claimType) {
        this.claimType = claimType;
    }

    public String getClaimStatus() {
        return claimStatus;
    }

    public void setClaimStatus(String claimStatus) {
        this.claimStatus = claimStatus;
    }

    public List<MercadoLibreOrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<MercadoLibreOrderItemDTO> items) {
        this.items = items;
    }
}
