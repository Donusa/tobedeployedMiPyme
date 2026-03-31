package com.mipyme.company;

import com.mipyme.mercadopago.model.MpChargeback;
import com.mipyme.mercadopago.model.MpClaim;
import com.mipyme.mercadopago.model.MpSubscription;
import com.mipyme.mercadopago.repository.MpChargebackRepository;
import com.mipyme.mercadopago.repository.MpClaimRepository;
import com.mipyme.mercadopago.repository.MpSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;


@Service
public class EntitlementService {

    private static final Logger logger = LoggerFactory.getLogger(EntitlementService.class);
    private static final int GRACE_DAYS = 5;

    private final CompanyRepository companyRepository;
    private final MpSubscriptionRepository subscriptionRepository;
    private final MpClaimRepository claimRepository;
    private final MpChargebackRepository chargebackRepository;
    private final PlanLimitService planLimitService;

    public EntitlementService(
            CompanyRepository companyRepository,
            MpSubscriptionRepository subscriptionRepository,
            MpClaimRepository claimRepository,
            MpChargebackRepository chargebackRepository,
            PlanLimitService planLimitService) {
        this.companyRepository = companyRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.claimRepository = claimRepository;
        this.chargebackRepository = chargebackRepository;
        this.planLimitService = planLimitService;
    }

    @Transactional
    public void recalculate(String tenantId) {
        logger.info("Recalculating entitlements for tenant: {}", tenantId);

        Optional<Company> optionalCompany = companyRepository.findByTenantSchema(tenantId);
        if (optionalCompany.isEmpty()) {
            optionalCompany = companyRepository.findByCompanyId(tenantId);
        }
        if (optionalCompany.isEmpty()) {
            logger.warn("Company not found for tenant identifier: {}", tenantId);
            return;
        }

        Company company = optionalCompany.get();
        Instant now = Instant.now();


        List<MpChargeback> chargebacks = chargebackRepository.findByTenantIdAndStatusNot(tenantId, "resolved");
        if (!chargebacks.isEmpty()) {
            transitionTo(company, Company.PlanStatus.BLOCKED, "CHARGEBACK");
            companyRepository.save(company);
            logger.info("Tenant {} BLOCKED — {} active chargebacks", tenantId, chargebacks.size());
            return;
        }


        List<MpClaim> openClaims = claimRepository.findByTenantIdAndStatusNot(tenantId, "resolved");
        if (!openClaims.isEmpty()) {
            transitionTo(company, Company.PlanStatus.IN_REVIEW, null);
            companyRepository.save(company);
            logger.info("Tenant {} → IN_REVIEW — {} open claims", tenantId, openClaims.size());
            return;
        }


        List<MpSubscription> subscriptions = subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId);
        MpSubscription latestSub = subscriptions.isEmpty() ? null : subscriptions.get(0);

        if (latestSub != null) {

            if (latestSub.getPlanTier() != null && !latestSub.getPlanTier().isBlank()) {
                String fullKey = latestSub.getPlanTier();


                String subTier = fullKey.replace("-annual", "");
                if (company.getPlanTier() == null || tierRank(subTier) >= tierRank(company.getPlanTier())) {
                    company.setPlanTier(subTier);
                    company.setBillingCycle(fullKey.endsWith("-annual") ? "annual" : "monthly");
                }
            }

            String mpStatus = latestSub.getStatus();

            if ("authorized".equalsIgnoreCase(mpStatus) || "active".equalsIgnoreCase(mpStatus)) {
                handleAuthorizedSubscription(company, latestSub, now);

            } else if ("paused".equalsIgnoreCase(mpStatus)) {

                if (company.getValidUntil() != null && now.isBefore(company.getValidUntil())) {

                    handlePaymentFailure(company, now);
                } else {
                    transitionTo(company, Company.PlanStatus.SUSPENDED, null);
                }

            } else if ("cancelled".equalsIgnoreCase(mpStatus)) {
                handleCancelledSubscription(company, now);

            } else if ("pending".equalsIgnoreCase(mpStatus)) {


                if (!isOperationalStatus(company.getPlanStatus()) && !hasPaidPeriod(company, now)) {
                    transitionTo(company, Company.PlanStatus.PENDING_ACTIVATION, null);
                }

            } else {

                logger.warn("Unknown MP subscription status '{}' for tenant {}", mpStatus, tenantId);
                handlePaymentFailure(company, now);
            }

        } else {

            handleNoSubscription(company, tenantId, now);
        }


        applyExpiryChecks(company, now);

        planLimitService.applyDowngradePolicies(tenantId);
        companyRepository.save(company);
        logger.info("Recalculation complete for tenant: {} → {}", tenantId, company.getPlanStatus());
    }



    private void handleAuthorizedSubscription(Company company, MpSubscription sub, Instant now) {

        if (sub.getNextPaymentDate() != null) {
            company.setValidUntil(sub.getNextPaymentDate());
        } else if (company.getValidUntil() == null || company.getValidUntil().isBefore(now)) {

            company.setValidUntil(now.plus(30, ChronoUnit.DAYS));
            logger.warn("No next_payment_date for subscription {} — using now+30d fallback", sub.getSubscriptionId());
        }


        company.setGraceUntil(company.getValidUntil().plus(GRACE_DAYS, ChronoUnit.DAYS));


        if (company.getPlanStatus() == Company.PlanStatus.BLOCKED
                || company.getPlanStatus() == Company.PlanStatus.BLOCKED_CANCELED
                || company.getPlanStatus() == Company.PlanStatus.BLOCKED_EXPIRED
                || company.getPlanStatus() == Company.PlanStatus.BLOCKED_PAYMENT_FAILED
                || company.getPlanStatus() == Company.PlanStatus.BLOCKED_TRIAL_EXPIRED
                || company.getPlanStatus() == Company.PlanStatus.PAST_DUE_GRACE
                || company.getPlanStatus() == Company.PlanStatus.PENDING_ACTIVATION
                || company.getPlanStatus() == Company.PlanStatus.TRIALING) {

            if (company.getPendingPlanKey() != null) {
                company.setPlanStatus(Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE);
            } else if (company.isCancelAtPeriodEnd()) {
                company.setPlanStatus(Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END);
            } else {
                company.setPlanStatus(Company.PlanStatus.ACTIVE);
            }
            company.setAccessBlockedReason(null);
        }

    }

    private void handleCancelledSubscription(Company company, Instant now) {
        if (company.getValidUntil() != null && now.isBefore(company.getValidUntil())) {

            if (company.getPlanStatus() == Company.PlanStatus.ACTIVE
                    || company.getPlanStatus() == Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE) {
                company.setPlanStatus(Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END);
                company.setCancelAtPeriodEnd(true);
            }

        } else {

            transitionTo(company, Company.PlanStatus.BLOCKED_CANCELED, "SUBSCRIPTION_CANCELED");
        }
    }

    private void handlePaymentFailure(Company company, Instant now) {
        if (isAlreadyBlocked(company.getPlanStatus())) {
            return;
        }

        if (company.getGraceUntil() == null) {

            company.setGraceUntil(now.plus(GRACE_DAYS, ChronoUnit.DAYS));
        }

        if (now.isBefore(company.getGraceUntil())) {
            transitionTo(company, Company.PlanStatus.PAST_DUE_GRACE, null);
        } else {
            transitionTo(company, Company.PlanStatus.BLOCKED_PAYMENT_FAILED, "PAYMENT_FAILED");
        }
    }

    private void handleNoSubscription(Company company, String tenantId, Instant now) {

        if (hasPaidPeriod(company, now)) {

            if (company.getPendingPlanKey() != null) {
                company.setPlanStatus(Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE);
            } else if (company.isCancelAtPeriodEnd()) {
                company.setPlanStatus(Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END);
            } else {
                company.setPlanStatus(Company.PlanStatus.ACTIVE);
            }
            return;
        }


        if (company.getTrialEnd() != null && now.isBefore(company.getTrialEnd())) {
            if (company.getPlanStatus() != Company.PlanStatus.TRIALING) {
                company.setPlanStatus(Company.PlanStatus.TRIALING);
                company.setAccessBlockedReason(null);
            }
            return;
        }


        if (company.getPlanStatus() == Company.PlanStatus.ACTIVE
                || company.getPlanStatus() == Company.PlanStatus.PENDING_ACTIVATION) {
            transitionTo(company, Company.PlanStatus.PENDING_ACTIVATION, null);
        }
    }

    private void applyExpiryChecks(Company company, Instant now) {

        if (company.getValidUntil() != null && now.isAfter(company.getValidUntil())) {
            if (company.getPlanStatus() == Company.PlanStatus.ACTIVE
                    || company.getPlanStatus() == Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE) {
                transitionTo(company, Company.PlanStatus.BLOCKED_EXPIRED, "SUBSCRIPTION_EXPIRED");
            }
            if (company.getPlanStatus() == Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END) {
                transitionTo(company, Company.PlanStatus.BLOCKED_CANCELED, "SUBSCRIPTION_CANCELED");
            }
        }


        if (company.getPlanStatus() == Company.PlanStatus.PAST_DUE_GRACE) {
            if (company.getGraceUntil() != null && now.isAfter(company.getGraceUntil())) {
                transitionTo(company, Company.PlanStatus.BLOCKED_PAYMENT_FAILED, "PAYMENT_FAILED");
            }
        }


        if (company.getPlanStatus() == Company.PlanStatus.TRIALING) {
            if (company.getTrialEnd() != null && now.isAfter(company.getTrialEnd())) {
                transitionTo(company, Company.PlanStatus.BLOCKED_TRIAL_EXPIRED, "TRIAL_EXPIRED");
            }
        }
    }



    private void transitionTo(Company company, Company.PlanStatus newStatus, String blockedReason) {
        company.setPlanStatus(newStatus);
        company.setAccessBlockedReason(blockedReason);
    }

    private boolean hasPaidPeriod(Company company, Instant now) {
        return company.getValidUntil() != null && now.isBefore(company.getValidUntil());
    }

    private boolean isOperationalStatus(Company.PlanStatus status) {
        return status == Company.PlanStatus.ACTIVE
                || status == Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE
                || status == Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END
                || status == Company.PlanStatus.TRIALING
                || status == Company.PlanStatus.PAST_DUE_GRACE;
    }

    private boolean isAlreadyBlocked(Company.PlanStatus status) {
        return status == Company.PlanStatus.BLOCKED
                || status == Company.PlanStatus.BLOCKED_CANCELED
                || status == Company.PlanStatus.BLOCKED_EXPIRED
                || status == Company.PlanStatus.BLOCKED_PAYMENT_FAILED
                || status == Company.PlanStatus.BLOCKED_TRIAL_EXPIRED;
    }

    private int tierRank(String tier) {
        return switch (tier.toLowerCase()) {
            case "enterprise" -> 2;
            case "pro"        -> 1;
            default           -> 0;
        };
    }
}
