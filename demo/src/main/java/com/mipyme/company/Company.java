package com.mipyme.company;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "companies", catalog = "mipyme")
public class Company {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String companyId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String tenantSchema;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false, unique = true, length = 6)
	private String ssoCode;

	@Column
	private String businessName;

	@Column(unique = true)
	private String cuit;

	@Column
	private String country;

	@Column
	private String province;

	@Column
	private String city;

	@Column
	private String industry;

	@Column
	private String email;

	@Column
	private String phone;

	@Column
	private String fiscalAddress;

	@Column(nullable = false)
	private boolean termsAccepted;

	@Enumerated(jakarta.persistence.EnumType.STRING)
	@Column(name = "plan_status", nullable = false, length = 40)
	private PlanStatus planStatus = PlanStatus.PENDING_PAYMENT;

	@Column(name = "plan_tier", length = 30)
	private String planTier;

	@Column(name = "valid_until")
	private Instant validUntil;

	@Column(name = "grace_until")
	private Instant graceUntil;

	@Column(name = "wpp_phone_number_id", unique = true)
	private String wppPhoneNumberId;

	@Column(name = "billing_cycle", length = 10)
	private String billingCycle;

	@Column(name = "pending_plan_key", length = 30)
	private String pendingPlanKey;




	@Column(name = "current_period_start")
	private Instant currentPeriodStart;


	@Column(name = "cancel_at_period_end", nullable = false)
	private boolean cancelAtPeriodEnd = false;


	@Column(name = "access_blocked_reason", length = 40)
	private String accessBlockedReason;


	@Column(name = "scheduled_change_type", length = 30)
	private String scheduledChangeType;


	@Column(name = "trial_end")
	private Instant trialEnd;


	@Column(name = "proration_amount", precision = 19, scale = 2)
	private BigDecimal prorationAmount;


	@Column(name = "proration_payment_id", length = 100)
	private String prorationPaymentId;


	@Column(name = "proration_status", length = 20)
	private String prorationStatus = "NONE";


	@Version
	@Column(columnDefinition = "BIGINT NOT NULL DEFAULT 0")
	private Long version = 0L;


	@PostLoad
	@PrePersist
	protected void ensureVersionNotNull() {
		if (version == null) version = 0L;
	}

	public enum PlanStatus {


		TRIAL,

		PENDING_PAYMENT,

		PAST_DUE,



		PENDING_ACTIVATION,

		TRIALING,

		ACTIVE,

		ACTIVE_SCHEDULED_CHANGE,

		ACTIVE_CANCEL_AT_PERIOD_END,

		PAST_DUE_GRACE,

		SUSPENDED,

		BLOCKED_TRIAL_EXPIRED,

		BLOCKED_EXPIRED,

		BLOCKED_CANCELED,

		BLOCKED_PAYMENT_FAILED,

		BLOCKED,

		IN_REVIEW
	}

	protected Company() {
	}

	public Company(String companyId, String name, String tenantSchema, Instant createdAt, String ssoCode,
			String businessName, String cuit, String country, String province, String city, String industry,
			String email, String phone, String fiscalAddress, boolean termsAccepted) {
		this.companyId = companyId;
		this.name = name;
		this.tenantSchema = tenantSchema;
		this.createdAt = createdAt;
		this.ssoCode = ssoCode;
		this.businessName = sanitize(businessName);
		this.cuit = sanitize(cuit);
		this.country = sanitize(country);
		this.province = sanitize(province);
		this.city = sanitize(city);
		this.industry = sanitize(industry);
		this.email = sanitize(email);
		this.phone = sanitize(phone);
		this.fiscalAddress = sanitize(fiscalAddress);
		this.termsAccepted = termsAccepted;
	}

	private String sanitize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value;
	}

	public Long getId() {
		return id;
	}

	public String getCompanyId() {
		return companyId;
	}

	public String getName() {
		return name;
	}

	public String getTenantSchema() {
		return tenantSchema;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public String getSsoCode() {
		return ssoCode;
	}

	public String getBusinessName() {
		return businessName;
	}

	public String getCuit() {
		return cuit;
	}

	public String getCountry() {
		return country;
	}

	public String getProvince() {
		return province;
	}

	public String getCity() {
		return city;
	}

	public String getIndustry() {
		return industry;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public String getFiscalAddress() {
		return fiscalAddress;
	}

	public boolean isTermsAccepted() {
		return termsAccepted;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setBusinessName(String businessName) {
		this.businessName = sanitize(businessName);
	}

	public void setCuit(String cuit) {
		this.cuit = sanitize(cuit);
	}

	public void setCountry(String country) {
		this.country = sanitize(country);
	}

	public void setProvince(String province) {
		this.province = sanitize(province);
	}

	public void setCity(String city) {
		this.city = sanitize(city);
	}

	public void setIndustry(String industry) {
		this.industry = sanitize(industry);
	}

	public void setEmail(String email) {
		this.email = sanitize(email);
	}

	public void setPhone(String phone) {
		this.phone = sanitize(phone);
	}

	public void setFiscalAddress(String fiscalAddress) {
		this.fiscalAddress = sanitize(fiscalAddress);
	}

	public PlanStatus getPlanStatus() {
		return planStatus;
	}

	public void setPlanStatus(PlanStatus planStatus) {
		this.planStatus = planStatus;
	}

	public Instant getValidUntil() {
		return validUntil;
	}

	public void setValidUntil(Instant validUntil) {
		this.validUntil = validUntil;
	}

	public Instant getGraceUntil() {
		return graceUntil;
	}

	public void setGraceUntil(Instant graceUntil) {
		this.graceUntil = graceUntil;
	}

	public String getWppPhoneNumberId() {
		return wppPhoneNumberId;
	}

	public void setWppPhoneNumberId(String wppPhoneNumberId) {
		this.wppPhoneNumberId = wppPhoneNumberId;
	}

	public String getPlanTier() {
		return planTier;
	}

	public void setPlanTier(String planTier) {
		this.planTier = planTier;
	}

	public String getBillingCycle() {
		return billingCycle;
	}

	public void setBillingCycle(String billingCycle) {
		this.billingCycle = billingCycle;
	}

	public String getPendingPlanKey() {
		return pendingPlanKey;
	}

	public void setPendingPlanKey(String pendingPlanKey) {
		this.pendingPlanKey = pendingPlanKey;
	}

	public Instant getCurrentPeriodStart() { return currentPeriodStart; }
	public void setCurrentPeriodStart(Instant currentPeriodStart) { this.currentPeriodStart = currentPeriodStart; }

	public boolean isCancelAtPeriodEnd() { return cancelAtPeriodEnd; }
	public void setCancelAtPeriodEnd(boolean cancelAtPeriodEnd) { this.cancelAtPeriodEnd = cancelAtPeriodEnd; }

	public String getAccessBlockedReason() { return accessBlockedReason; }
	public void setAccessBlockedReason(String accessBlockedReason) { this.accessBlockedReason = accessBlockedReason; }

	public String getScheduledChangeType() { return scheduledChangeType; }
	public void setScheduledChangeType(String scheduledChangeType) { this.scheduledChangeType = scheduledChangeType; }

	public Instant getTrialEnd() { return trialEnd; }
	public void setTrialEnd(Instant trialEnd) { this.trialEnd = trialEnd; }

	public BigDecimal getProrationAmount() { return prorationAmount; }
	public void setProrationAmount(BigDecimal prorationAmount) { this.prorationAmount = prorationAmount; }

	public String getProrationPaymentId() { return prorationPaymentId; }
	public void setProrationPaymentId(String prorationPaymentId) { this.prorationPaymentId = prorationPaymentId; }

	public String getProrationStatus() { return prorationStatus; }
	public void setProrationStatus(String prorationStatus) { this.prorationStatus = prorationStatus; }

	public Long getVersion() { return version; }


	public boolean isAccessBlocked() {
		return planStatus == PlanStatus.BLOCKED
				|| planStatus == PlanStatus.BLOCKED_CANCELED
				|| planStatus == PlanStatus.BLOCKED_EXPIRED
				|| planStatus == PlanStatus.BLOCKED_PAYMENT_FAILED
				|| planStatus == PlanStatus.BLOCKED_TRIAL_EXPIRED
				|| planStatus == PlanStatus.SUSPENDED
				|| planStatus == PlanStatus.PENDING_ACTIVATION
				|| planStatus == PlanStatus.PENDING_PAYMENT;
	}


	public boolean isPeriodExpired() {
		return validUntil != null && Instant.now().isAfter(validUntil);
	}
}
