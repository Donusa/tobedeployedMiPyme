package com.mipyme.mercadopago.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipyme.mercadopago.model.MpChargeback;
import com.mipyme.mercadopago.model.MpPayment;
import com.mipyme.mercadopago.repository.MpChargebackRepository;
import com.mipyme.mercadopago.repository.MpPaymentRepository;
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

@Service
public class MpChargebackProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MpChargebackProcessor.class);

    private final MpChargebackRepository chargebackRepository;
    private final MpPaymentRepository paymentRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EntitlementService entitlementService;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    public MpChargebackProcessor(MpChargebackRepository chargebackRepository, MpPaymentRepository paymentRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper, EntitlementService entitlementService) {
        this.chargebackRepository = chargebackRepository;
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.entitlementService = entitlementService;
    }

    public void processChargeback(String chargebackId) throws Exception {
        logger.info("Processing Chargeback ID: {}", chargebackId);

        String url = "https://api.mercadopago.com/v1/chargebacks/" + chargebackId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String responseBody = response.getBody();

            JsonNode root = objectMapper.readTree(responseBody);

            String status = root.path("status").asText(null);
            String paymentId = root.path("payment_id").asText(null);
            String reason = root.path("reason").asText(null);

            String tenantId = "UNRESOLVED";
            if (paymentId != null) {
                tenantId = paymentRepository.findByPaymentId(paymentId).map(MpPayment::getTenantId)
                        .orElse("UNRESOLVED");
            }

            MpChargeback chargeback = chargebackRepository.findByDisputeId(chargebackId).orElse(new MpChargeback());
            chargeback.setDisputeId(chargebackId);
            chargeback.setPaymentId(paymentId != null ? paymentId : "UNKNOWN");
            chargeback.setStatus(status);
            chargeback.setReason(reason);
            chargeback.setTenantId(tenantId);
            chargeback.setRawJson(responseBody);

            chargebackRepository.save(chargeback);
            logger.info("Saved MpChargeback {} with status '{}' for payment '{}'", chargebackId, status, paymentId);

            if (!"UNRESOLVED".equals(tenantId)) {
                entitlementService.recalculate(tenantId);
            }

        } catch (Exception e) {
            logger.error("Error fetching or processing chargeback " + chargebackId + " from MercadoPago API", e);
            throw e;
        }
    }
}
