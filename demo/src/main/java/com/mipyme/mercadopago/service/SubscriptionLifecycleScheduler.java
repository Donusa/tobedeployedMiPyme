package com.mipyme.mercadopago.service;

import com.mipyme.company.Company;
import com.mipyme.company.CompanyRepository;
import com.mipyme.company.EntitlementService;
import com.mipyme.company.SubscriptionChangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Service
public class SubscriptionLifecycleScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionLifecycleScheduler.class);

    private final CompanyRepository companyRepository;
    private final EntitlementService entitlementService;
    private final SubscriptionChangeService subscriptionChangeService;
    private final MpSubscriptionService subscriptionService;

    public SubscriptionLifecycleScheduler(
            CompanyRepository companyRepository,
            EntitlementService entitlementService,
            SubscriptionChangeService subscriptionChangeService,
            MpSubscriptionService subscriptionService) {
        this.companyRepository = companyRepository;
        this.entitlementService = entitlementService;
        this.subscriptionChangeService = subscriptionChangeService;
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(cron = "0 0 3 * * ?", zone = "America/Argentina/Buenos_Aires")
    public void runNightlyLifecycle() {
        logger.info("=== Nightly subscription lifecycle check starting ===");
        Instant now = Instant.now();

        processScheduledChanges(now);
        processCancelAtPeriodEnd(now);
        processExpiredPeriods(now);
        processExpiredGrace(now);
        processExpiredTrials(now);
        processOutstandingProrations();

        logger.info("=== Nightly subscription lifecycle check complete ===");
    }



    private void processScheduledChanges(Instant now) {
        List<Company> companies = companyRepository.findByPendingPlanKeyIsNotNull();
        if (companies.isEmpty()) return;

        logger.info("Processing {} companies with scheduled plan changes", companies.size());

        for (Company company : companies) {
            String tenantId = company.getTenantSchema();
            try {

                if (company.getValidUntil() == null || now.isAfter(company.getValidUntil())) {

                    String newPlanKey = company.getPendingPlanKey();
                    String previousStatus = company.getPlanStatus().name();
                    String previousPlanKey = (company.getPlanTier() != null ? company.getPlanTier() : "base")
                            + ("annual".equals(company.getBillingCycle()) ? "-annual" : "");
                    logger.info("Applying scheduled change for tenant {} → {}", tenantId, newPlanKey);


                    String payerEmail = resolvePayerEmail(tenantId);
                    if (payerEmail != null && !payerEmail.isBlank()) {
                        try {
                            subscriptionService.createSubscription(newPlanKey, payerEmail);
                        } catch (Exception subEx) {
                            logger.error("Failed to create new MP subscription for tenant {} (plan: {}): {}",
                                    tenantId, newPlanKey, subEx.getMessage());
                        }
                    }


                    String newTier = newPlanKey.replace("-annual", "");
                    String newCycle = newPlanKey.endsWith("-annual") ? "annual" : "monthly";
                    company.setPlanTier(newTier);
                    company.setBillingCycle(newCycle);
                    company.setPendingPlanKey(null);
                    company.setScheduledChangeType(null);
                    company.setPlanStatus(Company.PlanStatus.ACTIVE);
                    companyRepository.save(company);

                    logger.info("Scheduled change applied for tenant {} ({} → {})", tenantId, previousPlanKey, newPlanKey);
                }
            } catch (Exception e) {
                logger.error("Error applying scheduled change for tenant {}: {}", tenantId, e.getMessage(), e);
            }
        }
    }



    private void processCancelAtPeriodEnd(Instant now) {
        List<Company> canceling = companyRepository.findByCancelAtPeriodEndTrueAndPlanStatusIn(
                List.of(Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END,
                        Company.PlanStatus.ACTIVE,
                        Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE));

        for (Company company : canceling) {
            String tenantId = company.getTenantSchema();
            try {
                if (company.getValidUntil() == null || now.isAfter(company.getValidUntil())) {
                    company.setPlanStatus(Company.PlanStatus.BLOCKED_CANCELED);
                    company.setAccessBlockedReason("SUBSCRIPTION_CANCELED");
                    companyRepository.save(company);
                    logger.info("Tenant {} → BLOCKED_CANCELED (period ended after cancellation)", tenantId);
                }
            } catch (Exception e) {
                logger.error("Error processing cancel-at-period-end for tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }



    private void processExpiredPeriods(Instant now) {
        List<Company> active = companyRepository.findByPlanStatusIn(
                List.of(Company.PlanStatus.ACTIVE, Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE));

        for (Company company : active) {
            String tenantId = company.getTenantSchema();
            try {
                if (company.getValidUntil() != null && now.isAfter(company.getValidUntil())
                        && !company.isCancelAtPeriodEnd()) {

                    entitlementService.recalculate(tenantId);
                }
            } catch (Exception e) {
                logger.error("Error processing expired period for tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }



    private void processExpiredGrace(Instant now) {
        List<Company> inGrace = companyRepository.findByPlanStatusIn(
                List.of(Company.PlanStatus.PAST_DUE_GRACE));

        for (Company company : inGrace) {
            String tenantId = company.getTenantSchema();
            try {
                if (company.getGraceUntil() != null && now.isAfter(company.getGraceUntil())) {
                    company.setPlanStatus(Company.PlanStatus.BLOCKED_PAYMENT_FAILED);
                    company.setAccessBlockedReason("PAYMENT_FAILED");
                    companyRepository.save(company);
                    logger.info("Tenant {} → BLOCKED_PAYMENT_FAILED (grace expired)", tenantId);
                }
            } catch (Exception e) {
                logger.error("Error processing grace expiry for tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }



    private void processExpiredTrials(Instant now) {
        List<Company> trialing = companyRepository.findByPlanStatusIn(
                List.of(Company.PlanStatus.TRIALING));

        for (Company company : trialing) {
            String tenantId = company.getTenantSchema();
            try {
                if (company.getTrialEnd() != null && now.isAfter(company.getTrialEnd())) {
                    company.setPlanStatus(Company.PlanStatus.BLOCKED_TRIAL_EXPIRED);
                    company.setAccessBlockedReason("TRIAL_EXPIRED");
                    companyRepository.save(company);
                    logger.info("Tenant {} → BLOCKED_TRIAL_EXPIRED (trial ended)", tenantId);
                }
            } catch (Exception e) {
                logger.error("Error processing trial expiry for tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }



    private void processOutstandingProrations() {
        List<Company> withPendingProration = companyRepository.findByProrationStatus("PENDING");
        for (Company company : withPendingProration) {
            try {
                subscriptionChangeService.waveOutstandingProrations(company.getTenantSchema());
            } catch (Exception e) {
                logger.error("Error waiving proration for tenant {}: {}", company.getTenantSchema(), e.getMessage());
            }
        }
    }



    private String resolvePayerEmail(String tenantId) {

        return companyRepository.findByTenantSchema(tenantId)
                .map(c -> c.getEmail() != null ? c.getEmail() : "")
                .orElse("");
    }
}
