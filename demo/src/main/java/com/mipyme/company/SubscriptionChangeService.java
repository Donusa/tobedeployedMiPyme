package com.mipyme.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipyme.mercadopago.model.MpSubscription;
import com.mipyme.mercadopago.repository.MpSubscriptionRepository;
import com.mipyme.mercadopago.service.MpExchangeRateService;
import com.mipyme.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
public class SubscriptionChangeService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionChangeService.class);


    private static final Map<String, Integer> TIER_RANK = Map.of(
            "base", 0,
            "pro", 1,
            "enterprise", 2);

    private final CompanyRepository companyRepository;
    private final MpSubscriptionRepository subscriptionRepository;
    private final SubscriptionChangeAuditRepository auditRepository;
    private final ProrationCalculatorService prorationCalculator;
    private final MpExchangeRateService exchangeRateService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.back-url}")
    private String backUrl;


    @Value("${mercadopago.price-usd.base:10}")       private BigDecimal priceUsdBase;
    @Value("${mercadopago.price-usd.pro:30}")        private BigDecimal priceUsdPro;
    @Value("${mercadopago.price-usd.enterprise:60}") private BigDecimal priceUsdEnterprise;
    @Value("${mercadopago.price-usd.base-annual:100}")       private BigDecimal priceUsdBaseAnnual;
    @Value("${mercadopago.price-usd.pro-annual:300}")        private BigDecimal priceUsdProAnnual;
    @Value("${mercadopago.price-usd.enterprise-annual:600}") private BigDecimal priceUsdEnterpriseAnnual;

    public SubscriptionChangeService(
            CompanyRepository companyRepository,
            MpSubscriptionRepository subscriptionRepository,
            SubscriptionChangeAuditRepository auditRepository,
            ProrationCalculatorService prorationCalculator,
            MpExchangeRateService exchangeRateService,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.companyRepository = companyRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.auditRepository = auditRepository;
        this.prorationCalculator = prorationCalculator;
        this.exchangeRateService = exchangeRateService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }



    public record ChangeResult(
            String changeType,
            boolean immediate,
            String newPlanKey,
            Instant scheduledEffectiveAt,
            BigDecimal prorationAmount,
            String prorationInitPoint,
            String message) {
    }




    @Transactional
    public ChangeResult requestChange(String tenantId, String newPlanKey) {
        String normalizedKey = normalizeKey(newPlanKey);
        validatePlanKey(normalizedKey);

        Company company = loadCompany(tenantId);
        validateChangeAllowed(company, normalizedKey);

        String currentKey = buildCurrentKey(company);
        if (normalizedKey.equals(currentKey) && company.getPendingPlanKey() == null) {
            throw new IllegalStateException("El plan solicitado ya es el plan activo.");
        }

        String currentTier  = extractTier(currentKey);
        String newTier      = extractTier(normalizedKey);
        String currentCycle = extractCycle(currentKey);
        String newCycle     = extractCycle(normalizedKey);

        boolean tierChanged  = !currentTier.equals(newTier);
        boolean cycleChanged = !currentCycle.equals(newCycle);
        boolean isUpgrade    = tierChanged && rankOf(newTier) > rankOf(currentTier);
        boolean isDowngrade  = tierChanged && rankOf(newTier) < rankOf(currentTier);

        if (isUpgrade) {
            String changeType = cycleChanged ? "COMBINED_UPGRADE" : "UPGRADE";
            return applyImmediateUpgrade(tenantId, company, currentKey, normalizedKey, newCycle, changeType);
        } else if (isDowngrade) {
            String changeType = cycleChanged ? "COMBINED_DOWNGRADE" : "DOWNGRADE";
            return scheduleDeferred(tenantId, company, currentKey, normalizedKey, changeType);
        } else {

            return scheduleDeferred(tenantId, company, currentKey, normalizedKey, "FREQUENCY_ONLY");
        }
    }


    @Transactional
    public void cancelAtPeriodEnd(String tenantId) {
        Company company = loadCompany(tenantId);

        if (company.isCancelAtPeriodEnd()) {
            throw new IllegalStateException("La suscripción ya está programada para cancelarse al vencimiento.");
        }

        String previousStatus = company.getPlanStatus().name();


        cancelMpSubscription(tenantId);

        company.setCancelAtPeriodEnd(true);
        company.setPlanStatus(Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END);
        company.setPendingPlanKey(null);
        company.setScheduledChangeType(null);
        saveCompany(tenantId, company);

        recordAudit(tenantId, "CANCEL", buildCurrentKey(company), null,
                company.getValidUntil(), previousStatus, company.getPlanStatus().name(),
                null, "NONE", null, null, "USER");

        logger.info("Tenant {} scheduled cancel-at-period-end (validUntil: {})", tenantId, company.getValidUntil());
    }


    @Transactional
    public void revertCancelAtPeriodEnd(String tenantId) {
        Company company = loadCompany(tenantId);

        if (!company.isCancelAtPeriodEnd()) {
            throw new IllegalStateException("No hay cancelación programada para revertir.");
        }
        if (company.isPeriodExpired()) {
            throw new IllegalStateException("El período ya venció; no es posible revertir la cancelación.");
        }

        String previousStatus = company.getPlanStatus().name();

        company.setCancelAtPeriodEnd(false);

        if (company.getPendingPlanKey() != null) {
            company.setPlanStatus(Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE);
        } else {
            company.setPlanStatus(Company.PlanStatus.ACTIVE);
        }
        saveCompany(tenantId, company);

        recordAudit(tenantId, "REACTIVATE", buildCurrentKey(company), buildCurrentKey(company),
                null, previousStatus, company.getPlanStatus().name(),
                null, "NONE", null, null, "USER");



        logger.info("Tenant {} reverted cancel-at-period-end → {}", tenantId, company.getPlanStatus());
    }


    @Transactional
    public void cancelScheduledChange(String tenantId) {
        Company company = loadCompany(tenantId);

        if (company.getPendingPlanKey() == null) {
            throw new IllegalStateException("No hay cambio programado para cancelar.");
        }

        company.setPendingPlanKey(null);
        company.setScheduledChangeType(null);
        company.setPlanStatus(Company.PlanStatus.ACTIVE);
        saveCompany(tenantId, company);

        logger.info("Tenant {} cancelled scheduled change", tenantId);
    }


    @Transactional
    public void startTrial(String tenantId) {
        Company company = loadCompany(tenantId);

        if (company.getPlanStatus() == Company.PlanStatus.TRIALING) {
            return;
        }
        if (company.getTrialEnd() != null) {
            throw new IllegalStateException("El período de prueba ya fue utilizado.");
        }
        if (company.getPlanStatus() == Company.PlanStatus.ACTIVE) {
            throw new IllegalStateException("La cuenta ya tiene una suscripción activa.");
        }

        String previousStatus = company.getPlanStatus().name();
        Instant trialEnd = Instant.now().plus(28, ChronoUnit.DAYS);

        company.setTrialEnd(trialEnd);
        company.setValidUntil(trialEnd);
        company.setPlanTier("pro");
        company.setPlanStatus(Company.PlanStatus.TRIALING);
        company.setAccessBlockedReason(null);
        saveCompany(tenantId, company);

        recordAudit(tenantId, "TRIAL_START", null, "pro",
                trialEnd, previousStatus, "TRIALING",
                null, "NONE", null, null, "SYSTEM");

        logger.info("Trial started for tenant {}, ends {}", tenantId, trialEnd);
    }


    @Transactional
    public void convertTrialToActive(String tenantId, String paidPlanKey) {
        Company company = loadCompany(tenantId);
        String previousStatus = company.getPlanStatus().name();

        company.setPlanTier(extractTier(paidPlanKey));
        company.setBillingCycle(extractCycle(paidPlanKey).equals("annual") ? "annual" : "monthly");
        company.setPlanStatus(Company.PlanStatus.ACTIVE);
        company.setTrialEnd(null);
        company.setAccessBlockedReason(null);
        saveCompany(tenantId, company);

        recordAudit(tenantId, "TRIAL_CONVERTED", "pro", paidPlanKey,
                company.getValidUntil(), previousStatus, "ACTIVE",
                null, "NONE", null, null, "WEBHOOK");

        logger.info("Trial converted to ACTIVE for tenant {} → {}", tenantId, paidPlanKey);
    }


    @Transactional
    public void reactivateFromPayment(String tenantId, String planKey, Instant newValidUntil) {
        Company company = loadCompany(tenantId);
        String previousStatus = company.getPlanStatus().name();

        company.setPlanTier(extractTier(planKey));
        company.setBillingCycle(extractCycle(planKey).equals("annual") ? "annual" : "monthly");
        company.setPlanStatus(Company.PlanStatus.ACTIVE);
        company.setValidUntil(newValidUntil);
        company.setCancelAtPeriodEnd(false);
        company.setAccessBlockedReason(null);
        company.setGraceUntil(null);
        saveCompany(tenantId, company);

        recordAudit(tenantId, "REACTIVATE", null, planKey,
                newValidUntil, previousStatus, "ACTIVE",
                null, "NONE", null, null, "WEBHOOK");
    }


    @Transactional
    public void confirmProrationPayment(String tenantId, String mpPaymentId) {
        Optional<SubscriptionChangeAudit> auditOpt = auditRepository.findByProrationPaymentId(mpPaymentId);
        if (auditOpt.isEmpty()) {
            logger.warn("No proration audit found for payment {}", mpPaymentId);
            return;
        }

        SubscriptionChangeAudit audit = auditOpt.get();
        if (!"PENDING".equals(audit.getProrationStatus())) {
            logger.info("Proration {} already in status {} — skipping", mpPaymentId, audit.getProrationStatus());
            return;
        }

        audit.setProrationStatus("PAID");
        audit.setEffectiveAt(Instant.now());
        auditRepository.save(audit);


        Company company = loadCompany(tenantId);
        if ("PENDING".equals(company.getProrationStatus())) {
            company.setProrationStatus("PAID");
            company.setProrationPaymentId(mpPaymentId);
            saveCompany(tenantId, company);
        }

        logger.info("Proration confirmed PAID for tenant {} (payment {})", tenantId, mpPaymentId);
    }


    @Transactional
    public void waveOutstandingProrations(String tenantId) {
        auditRepository.findPendingProration(tenantId).ifPresent(audit -> {

            if (audit.getChangedAt().plus(48, ChronoUnit.HOURS).isBefore(Instant.now())) {
                audit.setProrationStatus("WAIVED");
                auditRepository.save(audit);
                logger.info("Proration WAIVED for tenant {} (audit id {})", tenantId, audit.getId());
            }
        });

        Company company = loadCompany(tenantId);
        if ("PENDING".equals(company.getProrationStatus())) {
            if (company.getValidUntil() != null) {
                company.setProrationStatus("WAIVED");
                company.setProrationAmount(null);
                saveCompany(tenantId, company);
            }
        }
    }



    private ChangeResult applyImmediateUpgrade(
            String tenantId, Company company,
            String currentKey, String newPlanKey,
            String newCycle, String changeType) {

        String previousStatus = company.getPlanStatus().name();
        BigDecimal exchangeRate = getExchangeRate();
        String     currentCycle = extractCycle(currentKey);



        BigDecimal currentPriceArs;
        BigDecimal prorationTargetPriceArs;
        if ("COMBINED_UPGRADE".equals(changeType)) {
            currentPriceArs = resolvePriceArs(currentKey, exchangeRate);

            String newTierCurrentCycle = "annual".equals(currentCycle)
                    ? extractTier(newPlanKey) + "-annual"
                    : extractTier(newPlanKey);
            prorationTargetPriceArs = resolvePriceArs(newTierCurrentCycle, exchangeRate);
        } else {
            currentPriceArs = resolvePriceArs(currentKey, exchangeRate);
            prorationTargetPriceArs = resolvePriceArs(newPlanKey, exchangeRate);
        }

        ProrationCalculatorService.ProrationResult proration = prorationCalculator.calculate(
                currentPriceArs, prorationTargetPriceArs, currentCycle,
                company.getValidUntil(), company.getCurrentPeriodStart());


        company.setPlanTier(extractTier(newPlanKey));

        if ("COMBINED_UPGRADE".equals(changeType)) {

            company.setScheduledChangeType("UPGRADE_MIGRATE");
            company.setPendingPlanKey(newPlanKey);
            company.setPlanStatus(Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE);
        } else {
            company.setBillingCycle(newCycle.equals("annual") ? "annual" : "monthly");
            company.setScheduledChangeType(null);
            company.setPendingPlanKey(null);
            company.setPlanStatus(Company.PlanStatus.ACTIVE);
        }
        company.setAccessBlockedReason(null);

        String prorationStatus = "NONE";
        String prorationInitPoint = null;
        BigDecimal prorationAmount = BigDecimal.ZERO;

        if (proration.requiresPayment()) {
            prorationAmount = proration.differential();
            prorationStatus = "PENDING";


            try {
                prorationInitPoint = createProrationCheckout(tenantId, prorationAmount, newPlanKey);
                company.setProrationAmount(prorationAmount);
                company.setProrationStatus("PENDING");
            } catch (Exception e) {

                logger.error("Failed to create proration checkout for tenant {}: {}", tenantId, e.getMessage());
                prorationStatus = "FAILED";
                company.setProrationStatus("FAILED");
            }




            String migrationPlanKey = "COMBINED_UPGRADE".equals(changeType)
                    ? ("annual".equals(currentCycle)
                        ? extractTier(newPlanKey) + "-annual"
                        : extractTier(newPlanKey))
                    : newPlanKey;
            migrateMpSubscription(tenantId, migrationPlanKey, company.getValidUntil());
        } else if (company.getValidUntil() != null) {

            String migrationPlanKey = "COMBINED_UPGRADE".equals(changeType)
                    ? ("annual".equals(currentCycle)
                        ? extractTier(newPlanKey) + "-annual"
                        : extractTier(newPlanKey))
                    : newPlanKey;
            migrateMpSubscription(tenantId, migrationPlanKey, company.getValidUntil());
        } else {

            logger.info("Tenant {} has no validUntil — skipping MP subscription migration (trial/pending upgrade)", tenantId);
        }

        saveCompany(tenantId, company);

        recordAudit(
                tenantId, changeType, currentKey, newPlanKey,
                null, previousStatus, company.getPlanStatus().name(),
                prorationAmount, prorationStatus, null, prorationInitPoint, "USER");


        if (prorationInitPoint != null) {

            logger.info("Proration checkout created for tenant {} — amount {} ARS", tenantId, prorationAmount);
        }

        String message = proration.requiresPayment()
                ? String.format("Plan %s activado. Diferencial %s ARS pendiente de pago.", newPlanKey, prorationAmount.toPlainString())
                : String.format("Plan %s activado. Sin cargo adicional.", newPlanKey);

        return new ChangeResult(changeType, true, newPlanKey, null,
                prorationAmount, prorationInitPoint, message);
    }



    private ChangeResult scheduleDeferred(String tenantId, Company company,
                                          String currentKey, String newPlanKey, String changeType) {
        String previousStatus = company.getPlanStatus().name();
        Instant effectiveAt = company.getValidUntil();

        company.setPendingPlanKey(newPlanKey);
        company.setScheduledChangeType(changeType);
        company.setPlanStatus(Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE);
        saveCompany(tenantId, company);

        recordAudit(tenantId, changeType, currentKey, newPlanKey,
                effectiveAt, previousStatus, "ACTIVE_SCHEDULED_CHANGE",
                null, "NONE", null, null, "USER");

        String message = String.format("Cambio a %s programado para el %s.", newPlanKey,
                effectiveAt != null ? effectiveAt.toString() : "fin del ciclo actual");

        return new ChangeResult(changeType, false, newPlanKey, effectiveAt,
                BigDecimal.ZERO, null, message);
    }



    private void cancelMpSubscription(String tenantId) {
        List<MpSubscription> subs = subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId);
        if (subs.isEmpty()) return;

        MpSubscription latest = subs.get(0);
        if ("cancelled".equalsIgnoreCase(latest.getStatus())) return;

        try {
            String url = "https://api.mercadopago.com/preapproval/" + latest.getSubscriptionId();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>("{\"status\":\"cancelled\"}", headers);
            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            latest.setStatus("cancelled");
            subscriptionRepository.save(latest);
            logger.info("Cancelled MP subscription {} for tenant {}", latest.getSubscriptionId(), tenantId);
        } catch (Exception e) {
            logger.error("Failed to cancel MP subscription for tenant {}: {}", tenantId, e.getMessage());
        }
    }


    private void migrateMpSubscription(String tenantId, String newPlanKey, Instant validUntil) {
        if (accessToken == null || accessToken.isBlank()) {
            logger.warn("No MP access token — skipping subscription migration for tenant {}", tenantId);
            return;
        }

        cancelMpSubscription(tenantId);


        int billingDay = 1;
        if (validUntil != null) {
            billingDay = validUntil
                    .atZone(java.time.ZoneId.of("America/Argentina/Buenos_Aires"))
                    .getDayOfMonth();
        }

        BigDecimal exchangeRate = getExchangeRate();
        BigDecimal arsAmount = resolvePriceArs(newPlanKey, exchangeRate);
        boolean isAnnual = newPlanKey.endsWith("-annual");

        try {
            Map<String, Object> autoRecurring = new HashMap<>();
            autoRecurring.put("frequency", isAnnual ? 12 : 1);
            autoRecurring.put("frequency_type", "months");
            autoRecurring.put("transaction_amount", arsAmount);
            autoRecurring.put("currency_id", "ARS");
            autoRecurring.put("billing_day", billingDay);

            Map<String, Object> body = new HashMap<>();
            body.put("auto_recurring", autoRecurring);
            body.put("external_reference", tenantId);
            body.put("reason", "MiPyme - " + formatPlanLabel(newPlanKey));
            body.put("back_url", backUrl);


            List<MpSubscription> existing = subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId);
            if (!existing.isEmpty() && existing.get(0).getPayerEmail() != null) {
                body.put("payer_email", existing.get(0).getPayerEmail());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            var response = restTemplate.exchange(
                    "https://api.mercadopago.com/preapproval", HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                var root = objectMapper.readTree(response.getBody());
                String newSubId = root.path("id").asText(null);
                if (newSubId != null) {
                    MpSubscription newSub = new MpSubscription();
                    newSub.setSubscriptionId(newSubId);
                    newSub.setStatus("pending");
                    newSub.setTenantId(tenantId);
                    newSub.setPlanTier(newPlanKey);
                    newSub.setAmount(arsAmount);
                    subscriptionRepository.save(newSub);
                    logger.info("Created new MP subscription {} for tenant {} (plan: {})", newSubId, tenantId, newPlanKey);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to create new MP subscription for tenant {}: {}", tenantId, e.getMessage());
        }
    }

    private String createProrationCheckout(String tenantId, BigDecimal amount, String newPlanKey) throws Exception {
        String url = "https://api.mercadopago.com/checkout/preferences";

        Map<String, Object> item = new HashMap<>();
        item.put("title", "MiPyme - Diferencial de upgrade a " + formatPlanLabel(newPlanKey));
        item.put("quantity", 1);
        item.put("unit_price", amount);
        item.put("currency_id", "ARS");

        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(item));
        body.put("external_reference", tenantId + ":PRORATION");
        body.put("back_urls", Map.of(
                "success", backUrl,
                "failure", backUrl,
                "pending", backUrl));
        body.put("auto_return", "approved");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        var response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        var root = objectMapper.readTree(response.getBody());
        return root.path("init_point").asText(null);
    }



    private void validateChangeAllowed(Company company, String newPlanKey) {
        Company.PlanStatus status = company.getPlanStatus();


        if (status == Company.PlanStatus.BLOCKED
                || status == Company.PlanStatus.IN_REVIEW) {
            throw new IllegalStateException(
                    "No es posible cambiar el plan mientras la cuenta está suspendida por contracargo o en revisión.");
        }
        if (status == Company.PlanStatus.BLOCKED_PAYMENT_FAILED
                || status == Company.PlanStatus.PAST_DUE_GRACE) {
            throw new IllegalStateException(
                    "Regularizá el pago pendiente antes de cambiar de plan.");
        }
        if (company.isCancelAtPeriodEnd()) {

            throw new IllegalStateException(
                    "Reactivá la suscripción antes de solicitar un cambio de plan.");
        }
    }



    private Company loadCompany(String tenantId) {
        TenantContext.clear();
        try {
            return companyRepository.findByTenantSchema(tenantId)
                    .orElseThrow(() -> new IllegalStateException("Company not found: " + tenantId));
        } finally {
            TenantContext.setCurrentTenant(tenantId);
        }
    }

    private void saveCompany(String tenantId, Company company) {
        TenantContext.clear();
        try {
            companyRepository.save(company);
        } finally {
            TenantContext.setCurrentTenant(tenantId);
        }
    }

    private SubscriptionChangeAudit recordAudit(
            String tenantId, String changeType,
            String fromKey, String toKey,
            Instant scheduledEffectiveAt,
            String previousStatus, String newStatus,
            BigDecimal prorationAmount, String prorationStatus,
            String prorationPaymentId, String prorationCheckoutUrl,
            String initiatedBy) {

        SubscriptionChangeAudit audit = SubscriptionChangeAudit.of(tenantId, changeType, initiatedBy);
        audit.setFromPlanKey(fromKey);
        audit.setToPlanKey(toKey);
        audit.setScheduledEffectiveAt(scheduledEffectiveAt);
        audit.setPreviousStatus(previousStatus);
        audit.setNewStatus(newStatus);
        audit.setProrationAmount(prorationAmount);
        audit.setProrationStatus(prorationStatus != null ? prorationStatus : "NONE");
        audit.setProrationPaymentId(prorationPaymentId);
        audit.setProrationCheckoutUrl(prorationCheckoutUrl);

        MpExchangeRateService.ExchangeRateInfo rateInfo = exchangeRateService.getLatestRate();
        if (rateInfo.rate() != null) {
            audit.setExchangeRateUsed(rateInfo.rate());
        }

        return auditRepository.save(audit);
    }

    private String buildCurrentKey(Company company) {
        String tier  = company.getPlanTier()  != null ? company.getPlanTier()  : "base";
        String cycle = company.getBillingCycle() != null ? company.getBillingCycle() : "monthly";
        return "annual".equals(cycle) ? tier + "-annual" : tier;
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }

    private void validatePlanKey(String key) {
        if (!List.of("base", "pro", "enterprise", "base-annual", "pro-annual", "enterprise-annual").contains(key)) {
            throw new IllegalStateException("Plan key inválido: " + key);
        }
    }

    private String extractTier(String planKey) {
        return planKey.replace("-annual", "");
    }

    private String extractCycle(String planKey) {
        return planKey.endsWith("-annual") ? "annual" : "monthly";
    }

    private int rankOf(String tier) {
        return TIER_RANK.getOrDefault(tier, -1);
    }

    private BigDecimal getExchangeRate() {
        MpExchangeRateService.ExchangeRateInfo info = exchangeRateService.getLatestRate();
        if (info.rate() == null || info.rate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Tipo de cambio no disponible. Intentá de nuevo.");
        }
        return info.rate();
    }

    private BigDecimal resolvePriceArs(String planKey, BigDecimal rate) {
        BigDecimal usd = switch (planKey) {
            case "base"             -> priceUsdBase;
            case "pro"              -> priceUsdPro;
            case "enterprise"       -> priceUsdEnterprise;
            case "base-annual"      -> priceUsdBaseAnnual;
            case "pro-annual"       -> priceUsdProAnnual;
            case "enterprise-annual"-> priceUsdEnterpriseAnnual;
            default -> throw new IllegalStateException("Unknown plan: " + planKey);
        };
        return usd.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatPlanLabel(String planKey) {
        if (planKey.endsWith("-annual")) {
            String base = planKey.replace("-annual", "");
            return capitalize(base) + " Anual";
        }
        return capitalize(planKey) + " Mensual";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
