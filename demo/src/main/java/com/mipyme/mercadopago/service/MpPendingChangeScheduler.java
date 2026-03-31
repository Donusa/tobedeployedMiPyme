package com.mipyme.mercadopago.service;

import com.mipyme.company.Company;
import com.mipyme.company.CompanyRepository;
import com.mipyme.company.EntitlementService;
import com.mipyme.mercadopago.model.MpSubscription;
import com.mipyme.mercadopago.repository.MpSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;



public class MpPendingChangeScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MpPendingChangeScheduler.class);

    private final CompanyRepository companyRepository;
    private final MpSubscriptionRepository subscriptionRepository;
    private final EntitlementService entitlementService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    public MpPendingChangeScheduler(CompanyRepository companyRepository,
            MpSubscriptionRepository subscriptionRepository,
            EntitlementService entitlementService,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.companyRepository = companyRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.entitlementService = entitlementService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void processPendingChanges() {
        List<Company> companies = companyRepository.findByPendingPlanKeyIsNotNull();
        if (companies.isEmpty()) {
            return;
        }

        logger.info("Processing {} companies with pending plan changes", companies.size());

        for (Company company : companies) {
            try {
                processPendingChange(company);
            } catch (Exception e) {
                logger.error("Error processing pending change for tenant {}: {}",
                        company.getTenantSchema(), e.getMessage(), e);
            }
        }
    }

    private void processPendingChange(Company company) {
        String tenant = company.getTenantSchema();


        entitlementService.recalculate(tenant);


        company = companyRepository.findByTenantSchema(tenant).orElse(company);



        if (company.getValidUntil() != null
                && company.getValidUntil().isBefore(Instant.now().plus(1, ChronoUnit.DAYS))) {

            List<MpSubscription> subs = subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(tenant);
            if (!subs.isEmpty()) {
                MpSubscription latest = subs.get(0);
                if ("authorized".equalsIgnoreCase(latest.getStatus())
                        || "active".equalsIgnoreCase(latest.getStatus())) {
                    cancelMpSubscription(latest, tenant);
                }
            }

            logger.info("Tenant {} pending change to '{}' — current subscription cancelled ahead of cycle end on {}",
                    tenant, company.getPendingPlanKey(), company.getValidUntil());
        }
    }

    private void cancelMpSubscription(MpSubscription sub, String tenant) {
        try {
            String url = "https://api.mercadopago.com/preapproval/" + sub.getSubscriptionId();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            String body = objectMapper.writeValueAsString(Map.of("status", "cancelled"));
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            sub.setStatus("cancelled");
            subscriptionRepository.save(sub);

            logger.info("Auto-cancelled subscription {} for tenant {} due to pending plan change",
                    sub.getSubscriptionId(), tenant);
        } catch (Exception e) {
            logger.error("Failed to cancel subscription {} for tenant {}: {}",
                    sub.getSubscriptionId(), tenant, e.getMessage());
        }
    }
}
