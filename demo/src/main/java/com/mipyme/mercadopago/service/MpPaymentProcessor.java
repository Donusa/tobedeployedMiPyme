package com.mipyme.mercadopago.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipyme.mercadopago.model.MpPayment;
import com.mipyme.mercadopago.repository.MpPaymentRepository;
import com.mipyme.company.EntitlementService;
import com.mipyme.company.SubscriptionChangeService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MpPaymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MpPaymentProcessor.class);

    private final MpPaymentRepository paymentRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EntitlementService entitlementService;
    private final SubscriptionChangeService subscriptionChangeService;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    public MpPaymentProcessor(MpPaymentRepository paymentRepository, RestTemplate restTemplate,
            ObjectMapper objectMapper, EntitlementService entitlementService,
            SubscriptionChangeService subscriptionChangeService) {
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.entitlementService = entitlementService;
        this.subscriptionChangeService = subscriptionChangeService;
    }

    public void processPayment(String paymentId) throws Exception {
        logger.info("Processing Payment ID: {}", paymentId);


        String url = "https://api.mercadopago.com/v1/payments/" + paymentId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String responseBody = response.getBody();

            JsonNode root = objectMapper.readTree(responseBody);

            String status = root.path("status").asText(null);
            String statusDetail = root.path("status_detail").asText(null);
            BigDecimal transactionAmount = new BigDecimal(root.path("transaction_amount").asText("0"));
            String currencyId = root.path("currency_id").asText(null);

            String dateApprovedStr = root.path("date_approved").asText(null);
            LocalDateTime approvedAt = null;
            if (dateApprovedStr != null && !dateApprovedStr.isBlank() && !dateApprovedStr.equals("null")) {
                approvedAt = LocalDateTime.parse(dateApprovedStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }

            String payerEmail = root.path("payer").path("email").asText(null);
            String externalReference = root.path("external_reference").asText(null);


            String tenantId = resolveTenantId(root, externalReference);

            if (tenantId == null) {
                logger.warn("Could not resolve tenant ID for payment {}. Payment will be saved with dummy tenant.",
                        paymentId);
                tenantId = "UNRESOLVED";
            }


            MpPayment payment = paymentRepository.findByPaymentId(paymentId).orElse(new MpPayment());
            payment.setPaymentId(paymentId);
            payment.setStatus(status);
            payment.setStatusDetail(statusDetail);
            payment.setTransactionAmount(transactionAmount);
            payment.setCurrencyId(currencyId);
            payment.setApprovedAt(approvedAt);
            payment.setPayerEmail(payerEmail);
            payment.setExternalReference(externalReference);
            payment.setTenantId(tenantId);
            payment.setRawJson(responseBody);

            paymentRepository.save(payment);
            logger.info("Saved MpPayment {} with status '{}' for tenant '{}'", paymentId, status, tenantId);


            if ("approved".equalsIgnoreCase(status) && !"UNRESOLVED".equals(tenantId)
                    && externalReference != null && externalReference.endsWith(":PRORATION")) {
                try {
                    subscriptionChangeService.confirmProrationPayment(tenantId, paymentId);
                    logger.info("Proration payment {} confirmed for tenant {}", paymentId, tenantId);
                } catch (Exception e) {
                    logger.error("Failed to confirm proration payment {} for tenant {}: {}",
                            paymentId, tenantId, e.getMessage());
                }
            }


            if (!"UNRESOLVED".equals(tenantId)) {
                entitlementService.recalculate(tenantId);
            }

        } catch (Exception e) {
            logger.error("Error fetching or processing payment " + paymentId + " from MercadoPago API", e);
            throw e;
        }
    }

    private String resolveTenantId(JsonNode root, String externalReference) {

        JsonNode metadata = root.path("metadata");
        if (!metadata.isMissingNode() && metadata.has("tenant_id")) {
            return metadata.get("tenant_id").asText();
        }


        if (externalReference != null && !externalReference.isBlank() && !externalReference.equals("null")) {

            int colonIdx = externalReference.indexOf(':');
            if (colonIdx > 0) {
                return externalReference.substring(0, colonIdx);
            }
            return externalReference;
        }

        return null;
    }
}
