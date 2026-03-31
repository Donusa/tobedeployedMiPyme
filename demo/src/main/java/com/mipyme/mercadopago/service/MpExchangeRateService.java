package com.mipyme.mercadopago.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipyme.mercadopago.model.MpExchangeRate;
import com.mipyme.mercadopago.model.MpPlanConfig;
import com.mipyme.mercadopago.repository.MpExchangeRateRepository;
import com.mipyme.mercadopago.repository.MpPlanConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MpExchangeRateService {

    private static final Logger logger = LoggerFactory.getLogger(MpExchangeRateService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MpExchangeRateRepository exchangeRateRepository;
    private final MpPlanConfigRepository planConfigRepository;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

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

    @Value("${mercadopago.exchange-rate.threshold:100}")
    private BigDecimal threshold;

    public MpExchangeRateService(RestTemplate restTemplate, ObjectMapper objectMapper,
            MpExchangeRateRepository exchangeRateRepository, MpPlanConfigRepository planConfigRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.exchangeRateRepository = exchangeRateRepository;
        this.planConfigRepository = planConfigRepository;
    }


    @EventListener(ApplicationReadyEvent.class)
    public void initExchangeRateOnStartup() {
        try {
            Optional<MpExchangeRate> existing = exchangeRateRepository.findTopByOrderByFetchedAtDesc();
            if (existing.isEmpty()) {
                logger.info("No exchange rate found on startup — fetching initial rate from dolarapi.com");
                RateResult result = fetchDollarRateWithSource();
                if (result != null && result.rate().compareTo(BigDecimal.ZERO) > 0) {
                    MpExchangeRate record = new MpExchangeRate(result.rate(), result.source());
                    exchangeRateRepository.save(record);
                    logger.info("Initial exchange rate stored: {} ARS/USD (source: {})", result.rate(), result.source());
                } else {
                    logger.warn("Could not fetch initial exchange rate from dolarapi.com on startup");
                }
            } else {
                logger.info("Exchange rate already stored ({} ARS/USD) — skipping startup fetch",
                        existing.get().getRate());
            }
        } catch (Exception e) {
            logger.error("Error during startup exchange rate initialization", e);
        }
    }


    @Scheduled(cron = "0 0 18 * * MON-FRI", zone = "America/Argentina/Buenos_Aires")
    public void updateExchangeRateAndPlans() {
        logger.info("Exchange rate check: starting daily closing validation");
        try {
            RateResult rateResult = fetchDollarRateWithSource();
            if (rateResult == null || rateResult.rate().compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Exchange rate check: could not fetch a valid rate");
                return;
            }
            BigDecimal currentRate = rateResult.rate();

            Optional<MpExchangeRate> latestOpt = exchangeRateRepository.findTopByOrderByFetchedAtDesc();

            MpExchangeRate record;
            if (latestOpt.isPresent()) {
                record = latestOpt.get();
                record.setRate(currentRate);
                record.setSource(rateResult.source());
                record.setFetchedAt(LocalDateTime.now());
            } else {
                record = new MpExchangeRate(currentRate, rateResult.source());
                record.setLastPlanUpdateRate(BigDecimal.ZERO);
            }

            BigDecimal lastUpdateRate = record.getLastPlanUpdateRate() != null
                    ? record.getLastPlanUpdateRate() : BigDecimal.ZERO;
            BigDecimal diff = currentRate.subtract(lastUpdateRate).abs();

            if (diff.compareTo(threshold) >= 0) {
                logger.info("Exchange rate change {} ARS exceeds threshold {} — updating MP plans and reference rate",
                        diff.setScale(0, RoundingMode.HALF_UP), threshold);
                updateAllPlans(currentRate);
                record.setLastPlanUpdateRate(currentRate);
            } else {
                logger.info("Exchange rate change {} ARS below threshold {} — no plan update needed",
                        diff.setScale(0, RoundingMode.HALF_UP), threshold);
            }

            exchangeRateRepository.save(record);

        } catch (Exception e) {
            logger.error("Exchange rate closing validation failed", e);
        }
    }


    public ExchangeRateInfo getLatestRate() {
        Optional<MpExchangeRate> opt = exchangeRateRepository.findTopByOrderByFetchedAtDesc();
        if (opt.isEmpty()) {
            logger.info("No exchange rate stored — attempting on-demand fetch");
            try {
                RateResult result = fetchDollarRateWithSource();
                if (result != null && result.rate().compareTo(BigDecimal.ZERO) > 0) {
                    MpExchangeRate record = new MpExchangeRate(result.rate(), result.source());
                    exchangeRateRepository.save(record);
                    logger.info("On-demand exchange rate stored: {} ARS/USD (source: {})", result.rate(), result.source());
                    return new ExchangeRateInfo(record.getRate(), record.getFetchedAt());
                }
            } catch (Exception e) {
                logger.error("On-demand exchange rate fetch failed", e);
            }
            return new ExchangeRateInfo(null, null);
        }
        MpExchangeRate r = opt.get();
        return new ExchangeRateInfo(r.getRate(), r.getFetchedAt());
    }


    public PlanPricingInfo getPlanPricing() {
        ExchangeRateInfo rateInfo = getLatestRate();
        BigDecimal rate = rateInfo.rate() != null ? rateInfo.rate() : BigDecimal.ZERO;

        List<MpPlanConfig> dbPlans = planConfigRepository.findAllByOrderByPlanKeyAsc();

        Map<String, PlanPrice> monthly = new LinkedHashMap<>();
        Map<String, PlanPrice> annual = new LinkedHashMap<>();

        if (!dbPlans.isEmpty()) {
            for (MpPlanConfig plan : dbPlans) {
                BigDecimal ars = plan.getPriceArs() != null ? plan.getPriceArs()
                        : (rate.compareTo(BigDecimal.ZERO) > 0
                                ? plan.getPriceUsd().multiply(rate).setScale(0, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO);
                PlanPrice pp = new PlanPrice(plan.getPriceUsd(), ars);
                String key = plan.getPlanKey();
                if (key.endsWith("-annual")) {
                    annual.put(key.replace("-annual", ""), pp);
                } else {
                    monthly.put(key, pp);
                }
            }
        } else {

            monthly.put("base", toPlanPrice(priceUsdBase, rate));
            monthly.put("pro", toPlanPrice(priceUsdPro, rate));
            monthly.put("enterprise", toPlanPrice(priceUsdEnterprise, rate));
            annual.put("base", toPlanPrice(priceUsdBaseAnnual, rate));
            annual.put("pro", toPlanPrice(priceUsdProAnnual, rate));
            annual.put("enterprise", toPlanPrice(priceUsdEnterpriseAnnual, rate));
        }

        return new PlanPricingInfo(rate, rateInfo.fetchedAt(), monthly, annual);
    }

    private PlanPrice toPlanPrice(BigDecimal usd, BigDecimal rate) {
        BigDecimal ars = rate.compareTo(BigDecimal.ZERO) > 0
                ? usd.multiply(rate).setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        return new PlanPrice(usd, ars);
    }

    private RateResult fetchDollarRateWithSource() {

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "https://dolarapi.com/v1/dolares/oficial", String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode node = objectMapper.readTree(response.getBody());
                BigDecimal venta = node.path("venta").decimalValue();
                if (venta != null && venta.compareTo(BigDecimal.ZERO) > 0) {
                    logger.info("Fetched oficial USD/ARS sell rate: {} ARS", venta);
                    return new RateResult(venta, "dolarapi.com/oficial");
                }
            }
        } catch (Exception e) {
            logger.warn("Oficial exchange rate source failed, trying blue fallback: {}", e.getMessage());
        }


        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "https://dolarapi.com/v1/dolares/blue", String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode node = objectMapper.readTree(response.getBody());
                BigDecimal venta = node.path("venta").decimalValue();
                if (venta != null && venta.compareTo(BigDecimal.ZERO) > 0) {
                    logger.info("Fetched blue USD/ARS sell rate (fallback): {} ARS", venta);
                    return new RateResult(venta, "dolarapi.com/blue");
                }
            }
        } catch (Exception e) {
            logger.error("All exchange rate sources failed", e);
        }

        return null;
    }

    private void updateAllPlans(BigDecimal rate) {
        List<MpPlanConfig> plans = planConfigRepository.findAllByOrderByPlanKeyAsc();
        if (plans.isEmpty()) {
            logger.warn("No plans in mp_plan_config table. Run POST /api/mercadopago/admin/provision-plans first.");
            return;
        }
        for (MpPlanConfig plan : plans) {
            BigDecimal arsAmount = plan.getPriceUsd().multiply(rate).setScale(0, RoundingMode.HALF_UP);
            updatePlan(plan.getMpPlanId(), arsAmount, plan.getLabel() != null ? plan.getLabel() : plan.getPlanKey());
            plan.setPriceArs(arsAmount);
            planConfigRepository.save(plan);
        }
    }

    private void updatePlan(String planId, BigDecimal arsAmount, String label) {
        if (planId == null || planId.isBlank()) {
            return;
        }
        if (accessToken == null || accessToken.isBlank()) {
            logger.warn("Skipping plan update for {} - no access token", label);
            return;
        }

        try {
            String url = "https://api.mercadopago.com/preapproval_plan/" + planId;

            Map<String, Object> autoRecurring = new LinkedHashMap<>();
            autoRecurring.put("transaction_amount", arsAmount);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("auto_recurring", autoRecurring);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Updated MP plan {} ({}): ARS {}", planId, label, arsAmount);
            } else {
                logger.warn("Failed to update MP plan {} ({}): HTTP {}", planId, label, response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error updating MP plan {} ({})", planId, label, e);
        }
    }

    public record ExchangeRateInfo(BigDecimal rate, LocalDateTime fetchedAt) {}
    public record PlanPrice(BigDecimal usd, BigDecimal ars) {}
    public record PlanPricingInfo(
            BigDecimal exchangeRate,
            LocalDateTime rateUpdatedAt,
            Map<String, PlanPrice> monthly,
            Map<String, PlanPrice> annual) {}
    private record RateResult(BigDecimal rate, String source) {}
}
