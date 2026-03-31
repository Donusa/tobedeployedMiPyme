package com.mipyme.tiendanube.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TiendaNubeOrderSummaryDTO {
    private Long id;

    @JsonProperty("number")
    private Long number;

    @JsonProperty("status")
    private String status;

    @JsonProperty("payment_status")
    private String paymentStatus;

    @JsonProperty("shipping_status")
    private String shippingStatus;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("total")
    private BigDecimal total;

    private String currency;

    @JsonProperty("customer")
    private Map<String, Object> customer;

    @JsonProperty("products")
    private List<TiendaNubeOrderItemDTO> products;

    @JsonProperty("payment_details")
    private Map<String, Object> paymentDetails;

    @JsonProperty("shipping_option")
    private String shippingOption;

    @JsonProperty("shipping_address")
    private Map<String, Object> shippingAddress;

    @JsonProperty("contact_email")
    private String contactEmail;



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getShippingStatus() {
        return shippingStatus;
    }

    public void setShippingStatus(String shippingStatus) {
        this.shippingStatus = shippingStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Map<String, Object> getCustomer() {
        return customer;
    }

    public void setCustomer(Map<String, Object> customer) {
        this.customer = customer;
    }

    public List<TiendaNubeOrderItemDTO> getProducts() {
        return products;
    }

    public void setProducts(List<TiendaNubeOrderItemDTO> products) {
        this.products = products;
    }

    public Map<String, Object> getPaymentDetails() {
        return paymentDetails;
    }

    public void setPaymentDetails(Map<String, Object> paymentDetails) {
        this.paymentDetails = paymentDetails;
    }

    public String getShippingOption() {
        return shippingOption;
    }

    public void setShippingOption(String shippingOption) {
        this.shippingOption = shippingOption;
    }

    public Map<String, Object> getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Map<String, Object> shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
}
