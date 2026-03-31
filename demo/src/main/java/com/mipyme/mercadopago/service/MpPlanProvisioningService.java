package com.mipyme.mercadopago.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipyme.mercadopago.model.MpPlanConfig;
import com.mipyme.mercadopago.repository.MpPlanConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class MpPlanProvisioningService {

    private static final Logger logger = LoggerFactory.getLogger(MpPlanProvisioningService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MpPlanConfigRepository planConfigRepository;
    private final MpExchangeRateService exchangeRateService;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.back-url:http://localhost:4200/configuracion/facturacion/checkout-result}")
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

    public MpPlanProvisioningService(RestTemplate restTemplate, ObjectMapper objectMapper,
            MpPlanConfigRepository planConfigRepository, MpExchangeRateService exchangeRateService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.planConfigRepository = planConfigRepository;
        this.exchangeRateService = exchangeRateService;
    }


    public List<PlanProvisionResult> provisionAllPlans() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("MP_ACCESS_TOKEN is not configured");
        }
        if (backUrl == null || backUrl.isBlank()) {
            throw new IllegalStateException("MP_BACK_URL is not configured. Set it to your checkout result URL (e.g. https://tudominio.com/configuracion/facturacion/checkout-result)");
        }

        BigDecimal rate = getExchangeRate();

        List<PlanDefinition> definitions = List.of(
            new PlanDefinition("base", "MiPyme - Plan Base", priceUsdBase, 1, "months"),
            new PlanDefinition("pro", "MiPyme - Plan Pro", priceUsdPro, 1, "months"),
            new PlanDefinition("enterprise", "MiPyme - Plan Enterprise", priceUsdEnterprise, 1, "months"),
            new PlanDefinition("base-annual", "MiPyme - Plan Base Anual", priceUsdBaseAnnual, 12, "months"),
            new PlanDefinition("pro-annual", "MiPyme - Plan Pro Anual", priceUsdProAnnual, 12, "months"),
            new PlanDefinition("enterprise-annual", "MiPyme - Plan Enterprise Anual", priceUsdEnterpriseAnnual, 12, "months")
        );

        List<PlanProvisionResult> results = new ArrayList<>();

        for (PlanDefinition def : definitions) {
            Optional<MpPlanConfig> existing = planConfigRepository.findByPlanKey(def.key());
            if (existing.isPresent()) {
                results.add(new PlanProvisionResult(def.key(), existing.get().getMpPlanId(), "SKIPPED - already exists"));
                continue;
            }

            try {
                BigDecimal arsAmount = def.priceUsd().multiply(rate).setScale(0, RoundingMode.HALF_UP);
                String mpPlanId = createPlanInMercadoPago(def.reason(), arsAmount, def.frequency(), def.frequencyType());

                MpPlanConfig config = new MpPlanConfig(
                        def.key(), mpPlanId, def.priceUsd(), arsAmount,
                        def.frequency(), def.frequencyType(), def.reason());
                planConfigRepository.save(config);

                results.add(new PlanProvisionResult(def.key(), mpPlanId, "CREATED"));
                logger.info("Provisioned MP plan: {} -> {} (ARS {})", def.key(), mpPlanId, arsAmount);

            } catch (Exception e) {
                logger.error("Failed to provision plan: {}", def.key(), e);
                results.add(new PlanProvisionResult(def.key(), null, "FAILED: " + e.getMessage()));
            }
        }

        return results;
    }

    private String createPlanInMercadoPago(String reason, BigDecimal arsAmount, int frequency, String frequencyType) throws Exception {
        String url = "https://api.mercadopago.com/preapproval_plan";

        Map<String, Object> autoRecurring = new LinkedHashMap<>();
        autoRecurring.put("frequency", frequency);
        autoRecurring.put("frequency_type", frequencyType);
        autoRecurring.put("transaction_amount", arsAmount);
        autoRecurring.put("currency_id", "ARS");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reason", reason);
        body.put("auto_recurring", autoRecurring);
        body.put("back_url", backUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("MercadoPago returned " + response.getStatusCode() + ": " + response.getBody());
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        String planId = root.path("id").asText(null);
        if (planId == null || planId.isBlank()) {
            throw new RuntimeException("MercadoPago did not return a plan ID");
        }

        return planId;
    }

    private BigDecimal getExchangeRate() {
        MpExchangeRateService.ExchangeRateInfo info = exchangeRateService.getLatestRate();
        if (info.rate() != null && info.rate().compareTo(BigDecimal.ZERO) > 0) {
            return info.rate();
        }

        logger.info("No exchange rate in DB, triggering fetch for plan provisioning");
        exchangeRateService.updateExchangeRateAndPlans();
        info = exchangeRateService.getLatestRate();
        if (info.rate() != null && info.rate().compareTo(BigDecimal.ZERO) > 0) {
            return info.rate();
        }
        throw new IllegalStateException("Could not obtain USD/ARS exchange rate. Check internet connectivity.");
    }

    public record PlanDefinition(String key, String reason, BigDecimal priceUsd, int frequency, String frequencyType) {}
    public record PlanProvisionResult(String planKey, String mpPlanId, String status) {}
}
