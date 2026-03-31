package com.mipyme.mercadopago.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipyme.mercadopago.model.MpPlanConfig;
import com.mipyme.mercadopago.model.MpSubscription;
import com.mipyme.mercadopago.repository.MpPlanConfigRepository;
import com.mipyme.mercadopago.repository.MpSubscriptionRepository;
import com.mipyme.company.EntitlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MpSubscriptionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MpSubscriptionProcessor.class);

    private final MpSubscriptionRepository subscriptionRepository;
    private final MpPlanConfigRepository planConfigRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EntitlementService entitlementService;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    public MpSubscriptionProcessor(MpSubscriptionRepository subscriptionRepository,
            MpPlanConfigRepository planConfigRepository, RestTemplate restTemplate,
            ObjectMapper objectMapper, EntitlementService entitlementService) {
        this.subscriptionRepository = subscriptionRepository;
        this.planConfigRepository = planConfigRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.entitlementService = entitlementService;
    }

    public void processSubscription(String subscriptionId, String type) throws Exception {
        logger.info("Processing Subscription ID: {} with type: {}", subscriptionId, type);


        String endpoint = type.equals("subscription_preapproval_plan") ? "preapproval_plan" : "preapproval";
        String url = "https://api.mercadopago.com/" + endpoint + "/" + subscriptionId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String responseBody = response.getBody();

            JsonNode root = objectMapper.readTree(responseBody);

            String status = root.path("status").asText(null);
            String payerEmail = root.path("payer_email").asText(null);

            BigDecimal amount = BigDecimal.ZERO;
            String period = null;

            JsonNode recurring = root.path("auto_recurring");
            if (!recurring.isMissingNode()) {
                amount = new BigDecimal(recurring.path("transaction_amount").asText("0"));
                period = recurring.path("frequency").asText() + " " + recurring.path("frequency_type").asText();
            }

            String externalReference = root.path("external_reference").asText(null);
            String reason = root.path("reason").asText(null);

            String tenantId = resolveTenantId(root, externalReference, reason);

            if (tenantId == null) {
                logger.warn("Could not resolve tenant ID for subscription {}. Using dummy tenant.", subscriptionId);
                tenantId = "UNRESOLVED";
            }

            MpSubscription subscription = subscriptionRepository.findBySubscriptionId(subscriptionId)
                    .orElse(new MpSubscription());
            subscription.setSubscriptionId(subscriptionId);
            subscription.setStatus(status);
            subscription.setAmount(amount);
            subscription.setPeriod(period);
            subscription.setPayerEmail(payerEmail);
            subscription.setTenantId(tenantId);
            subscription.setRawJson(responseBody);


            subscription.setNextPaymentDate(parseInstantField(root, "next_payment_date"));
            subscription.setLastChargedDate(parseInstantField(root, "date_last_charged"));


            JsonNode recurringNode = root.path("auto_recurring");
            if (!recurringNode.isMissingNode() && recurringNode.has("billing_day")) {
                subscription.setBillingDay(recurringNode.path("billing_day").asInt(0));
            }


            String preapprovalPlanId = root.path("preapproval_plan_id").asText(null);
            if (preapprovalPlanId != null && subscription.getPlanTier() == null) {
                String tier = resolvePlanTierFromMpPlanId(preapprovalPlanId);
                if (tier != null) {
                    subscription.setPlanTier(tier);
                }
            }

            subscriptionRepository.save(subscription);
            logger.info("Saved MpSubscription {} with status '{}' planTier '{}' for tenant '{}'",
                    subscriptionId, status, subscription.getPlanTier(), tenantId);

            if (!"UNRESOLVED".equals(tenantId)) {
                entitlementService.recalculate(tenantId);
            }

        } catch (Exception e) {
            logger.error("Error fetching or processing subscription " + subscriptionId + " from MercadoPago API", e);
            throw e;
        }
    }

    private String resolveTenantId(JsonNode root, String externalReference, String reason) {
        if (externalReference != null && !externalReference.isBlank() && !externalReference.equals("null")) {
            return externalReference;
        }
        return null;
    }

    private String resolvePlanTierFromMpPlanId(String mpPlanId) {
        List<MpPlanConfig> plans = planConfigRepository.findAllByOrderByPlanKeyAsc();
        for (MpPlanConfig plan : plans) {
            if (mpPlanId.equals(plan.getMpPlanId())) {
                return plan.getPlanKey();
            }
        }
        logger.warn("Could not resolve plan tier for MP plan ID: {}", mpPlanId);
        return null;
    }


    private Instant parseInstantField(JsonNode root, String fieldName) {
        String value = root.path(fieldName).asText(null);
        if (value == null || value.isBlank() || value.equals("null")) return null;
        try {
            return Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(value));
        } catch (Exception e) {
            logger.debug("Could not parse date field '{}': {}", fieldName, value);
            return null;
        }
    }
}
