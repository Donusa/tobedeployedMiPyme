package com.mipyme.mercadopago.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipyme.company.Company;
import com.mipyme.company.CompanyRepository;
import com.mipyme.company.SubscriptionChangeService;
import com.mipyme.mercadopago.model.MpSubscription;
import com.mipyme.mercadopago.repository.MpSubscriptionRepository;
import com.mipyme.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MpSubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(MpSubscriptionService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CompanyRepository companyRepository;
    private final MpSubscriptionRepository subscriptionRepository;
    private final MpExchangeRateService exchangeRateService;
    private final SubscriptionChangeService subscriptionChangeService;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.back-url}")
    private String backUrl;

    @Value("${mercadopago.price-usd.base:10}")
    private BigDecimal priceUsdBase;
    @Value("${mercadopago.price-usd.pro:30}")
    private BigDecimal priceUsdPro;
    @Value("${mercadopago.price-usd.enterprise:60}")
    private BigDecimal priceUsdEnterprise;
    @Value("${mercadopago.price-usd.base-annual:100}")
    private BigDecimal priceUsdBaseAnnual;
    @Value("${mercadopago.price-usd.pro-annual:300}")
    private BigDecimal priceUsdProAnnual;
    @Value("${mercadopago.price-usd.enterprise-annual:600}")
    private BigDecimal priceUsdEnterpriseAnnual;

    public MpSubscriptionService(RestTemplate restTemplate, ObjectMapper objectMapper,
            CompanyRepository companyRepository, MpSubscriptionRepository subscriptionRepository,
            MpExchangeRateService exchangeRateService, SubscriptionChangeService subscriptionChangeService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.companyRepository = companyRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.exchangeRateService = exchangeRateService;
        this.subscriptionChangeService = subscriptionChangeService;
    }


    public String createSubscription(String planType, String payerEmail) throws Exception {
        String tenantSchema = TenantContext.getCurrentTenant();
        if (tenantSchema == null) {
            throw new IllegalStateException("No tenant context available");
        }

        TenantContext.clear();
        Company company;
        try {
            company = companyRepository.findByTenantSchema(tenantSchema)
                    .orElseThrow(() -> new IllegalStateException("Company not found for tenant: " + tenantSchema));
        } finally {
            TenantContext.setCurrentTenant(tenantSchema);
        }

        BigDecimal arsAmount = resolvePlanAmount(planType);
        boolean isAnnual = planType.toLowerCase().endsWith("-annual");

        String url = "https://api.mercadopago.com/preapproval";

        Map<String, Object> autoRecurring = new HashMap<>();
        autoRecurring.put("frequency", isAnnual ? 12 : 1);
        autoRecurring.put("frequency_type", "months");
        autoRecurring.put("transaction_amount", arsAmount);
        autoRecurring.put("currency_id", "ARS");

        Map<String, Object> body = new HashMap<>();
        body.put("auto_recurring", autoRecurring);
        body.put("payer_email", payerEmail);
        body.put("external_reference", company.getTenantSchema());
        body.put("reason", "MiPyme - Plan " + formatPlanLabel(planType));
        body.put("back_url", backUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            logger.error("MercadoPago preapproval creation failed: {}", response.getBody());
            throw new RuntimeException("MercadoPago returned status " + response.getStatusCode());
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        String initPoint = root.path("init_point").asText(null);
        String subscriptionId = root.path("id").asText(null);

        if (initPoint == null || initPoint.isBlank()) {
            throw new RuntimeException("MercadoPago did not return an init_point");
        }



        MpSubscription pendingSub = subscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElse(new MpSubscription());
        pendingSub.setSubscriptionId(subscriptionId);
        pendingSub.setStatus("pending");
        pendingSub.setPayerEmail(payerEmail);
        pendingSub.setTenantId(tenantSchema);
        pendingSub.setPlanTier(planType.toLowerCase());
        subscriptionRepository.save(pendingSub);


        boolean isAnn = planType.toLowerCase().endsWith("-annual");
        company.setBillingCycle(isAnn ? "annual" : "monthly");
        company.setPendingPlanKey(null);
        TenantContext.clear();
        try {
            companyRepository.save(company);
        } finally {
            TenantContext.setCurrentTenant(tenantSchema);
        }

        logger.info("Created MP subscription {} for tenant {} (plan: {}), saved pending record",
                subscriptionId, tenantSchema, planType.toLowerCase());
        return initPoint;
    }


    public void cancelSubscriptionIfActive(String tenantSchema) throws Exception {
        List<MpSubscription> subs = subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(tenantSchema);
        if (subs.isEmpty()) {
            logger.info("No MP subscription found for tenant {} — nothing to cancel", tenantSchema);
            return;
        }
        MpSubscription latest = subs.get(0);
        if ("cancelled".equalsIgnoreCase(latest.getStatus())) {
            logger.info("MP subscription {} is already cancelled for tenant {}", latest.getSubscriptionId(), tenantSchema);
            return;
        }
        cancelSubscription(tenantSchema);
    }


    public void cancelSubscription(String tenantSchema) {
        subscriptionChangeService.cancelAtPeriodEnd(tenantSchema);
        logger.info("Subscription cancel-at-period-end applied for tenant {}", tenantSchema);
    }


    public void pauseSubscription(String tenantSchema) throws Exception {
        List<MpSubscription> subs = subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(tenantSchema);
        if (subs.isEmpty()) {
            throw new IllegalStateException("No subscription found for tenant: " + tenantSchema);
        }

        MpSubscription latest = subs.get(0);
        String url = "https://api.mercadopago.com/preapproval/" + latest.getSubscriptionId();

        Map<String, Object> body = new HashMap<>();
        body.put("status", "paused");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("MercadoPago returned status " + response.getStatusCode());
        }

        latest.setStatus("paused");
        subscriptionRepository.save(latest);
        logger.info("Paused MP subscription {} for tenant {}", latest.getSubscriptionId(), tenantSchema);
    }


    public SubscriptionStatusDto getSubscriptionStatus(String tenantSchema) {
        TenantContext.clear();
        Company company;
        try {
            Optional<Company> opt = companyRepository.findByTenantSchema(tenantSchema);
            if (opt.isEmpty()) {
                return new SubscriptionStatusDto("unknown", null, null, null, null, null, null, null, null, null, null, false, null, null, null);
            }
            company = opt.get();
        } finally {
            TenantContext.setCurrentTenant(tenantSchema);
        }

        List<MpSubscription> subs = subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(tenantSchema);
        MpSubscription latestSub = subs.isEmpty() ? null : subs.get(0);

        String planTier = company.getPlanTier();
        if (planTier == null && latestSub != null && latestSub.getPlanTier() != null) {

            planTier = latestSub.getPlanTier().replace("-annual", "");
        }

        String billingCycle = company.getBillingCycle();
        if (billingCycle == null && latestSub != null && latestSub.getPlanTier() != null) {
            billingCycle = latestSub.getPlanTier().endsWith("-annual") ? "annual" : "monthly";
        }

        return new SubscriptionStatusDto(
                company.getPlanStatus().name().toLowerCase(),
                company.getValidUntil() != null ? company.getValidUntil().toString() : null,
                company.getGraceUntil() != null ? company.getGraceUntil().toString() : null,
                latestSub != null ? latestSub.getStatus() : null,
                latestSub != null ? latestSub.getSubscriptionId() : null,
                latestSub != null && latestSub.getAmount() != null ? latestSub.getAmount().toPlainString() : null,
                planTier,
                billingCycle,
                company.getPendingPlanKey(),
                company.getAccessBlockedReason(),
                company.getScheduledChangeType(),
                company.isCancelAtPeriodEnd(),
                company.getTrialEnd() != null ? company.getTrialEnd().toString() : null,
                company.getProrationAmount() != null ? company.getProrationAmount().toPlainString() : null,
                company.getProrationStatus());
    }

    private BigDecimal resolvePlanAmount(String planType) {
        BigDecimal usdPrice = switch (planType.toLowerCase()) {
            case "base" -> priceUsdBase;
            case "pro" -> priceUsdPro;
            case "enterprise" -> priceUsdEnterprise;
            case "base-annual" -> priceUsdBaseAnnual;
            case "pro-annual" -> priceUsdProAnnual;
            case "enterprise-annual" -> priceUsdEnterpriseAnnual;
            default -> throw new IllegalStateException("Unknown plan type: " + planType);
        };
        MpExchangeRateService.ExchangeRateInfo rateInfo = exchangeRateService.getLatestRate();
        if (rateInfo.rate() == null || rateInfo.rate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Exchange rate not available. Please try again shortly.");
        }
        return usdPrice.multiply(rateInfo.rate()).setScale(2, RoundingMode.HALF_UP);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private String formatPlanLabel(String planType) {
        if (planType.toLowerCase().endsWith("-annual")) {
            String base = planType.substring(0, planType.length() - 7);
            return capitalize(base) + " Anual";
        }
        return capitalize(planType);
    }


    public SubscriptionChangeService.ChangeResult requestPlanChange(String tenantSchema, String newPlanKey) {
        return subscriptionChangeService.requestChange(tenantSchema, newPlanKey);
    }


    @Deprecated
    public void scheduleChange(String tenantSchema, String newPlanKey) {
        subscriptionChangeService.requestChange(tenantSchema, newPlanKey);
    }


    public void cancelScheduledChange(String tenantSchema) {
        subscriptionChangeService.cancelScheduledChange(tenantSchema);
        logger.info("Scheduled change cancelled for tenant {}", tenantSchema);
    }

    public record SubscriptionStatusDto(
            String planStatus,
            String validUntil,
            String graceUntil,
            String subscriptionStatus,
            String subscriptionId,
            String amount,
            String planTier,
            String billingCycle,
            String pendingPlanKey,
            String accessBlockedReason,
            String scheduledChangeType,
            boolean cancelAtPeriodEnd,
            String trialEnd,
            String prorationAmount,
            String prorationStatus) {
    }
}
