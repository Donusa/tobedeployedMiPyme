package com.mipyme.mercadopago.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipyme.mercadopago.model.MpClaim;
import com.mipyme.mercadopago.model.MpPayment;
import com.mipyme.mercadopago.repository.MpClaimRepository;
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
public class MpClaimProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MpClaimProcessor.class);

    private final MpClaimRepository claimRepository;
    private final MpPaymentRepository paymentRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EntitlementService entitlementService;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    public MpClaimProcessor(MpClaimRepository claimRepository, MpPaymentRepository paymentRepository,
            RestTemplate restTemplate, ObjectMapper objectMapper,
            EntitlementService entitlementService) {
        this.claimRepository = claimRepository;
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.entitlementService = entitlementService;
    }

    public void processClaim(String claimId) throws Exception {
        logger.info("Processing Claim ID: {}", claimId);



        String url = "https://api.mercadopago.com/v1/claims/" + claimId;

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
            String stage = root.path("stage").asText(null);




            String tenantId = "UNRESOLVED";
            if (paymentId != null) {
                tenantId = paymentRepository.findByPaymentId(paymentId).map(MpPayment::getTenantId)
                        .orElse("UNRESOLVED");
            }

            MpClaim claim = claimRepository.findByDisputeId(claimId).orElse(new MpClaim());
            claim.setDisputeId(claimId);
            claim.setPaymentId(paymentId != null ? paymentId : "UNKNOWN");
            claim.setStatus(status);
            claim.setReason(reason);
            claim.setStage(stage);
            claim.setTenantId(tenantId);
            claim.setRawJson(responseBody);

            claimRepository.save(claim);
            logger.info("Saved MpClaim {} with status '{}' for payment '{}'", claimId, status, paymentId);


            if (!"UNRESOLVED".equals(tenantId)) {
                entitlementService.recalculate(tenantId);
            }

        } catch (Exception e) {
            logger.error("Error fetching or processing claim " + claimId + " from MercadoPago API", e);
            throw e;
        }
    }
}
