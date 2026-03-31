package com.mipyme.whatsapp.service;

import com.mipyme.company.Company;
import com.mipyme.company.CompanyRepository;
import com.mipyme.tenant.TenantContext;
import com.mipyme.whatsapp.model.WppConnectSession;
import com.mipyme.whatsapp.model.WppConnection;
import com.mipyme.whatsapp.repository.WppConnectSessionRepository;
import com.mipyme.whatsapp.repository.WppConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WppConnectService {

    private static final Logger logger = LoggerFactory.getLogger(WppConnectService.class);

    @Value("${whatsapp.meta-app-id:}")
    private String metaAppId;

    @Value("${whatsapp.meta-app-secret:}")
    private String metaAppSecret;

    @Value("${whatsapp.meta-config-id:}")
    private String metaConfigId;

    @Value("${whatsapp.graph-version:v18.0}")
    private String graphVersion;

    @Value("${whatsapp.connect-session-ttl-min:15}")
    private int sessionTtlMinutes;

    private final WppConnectSessionRepository sessionRepository;
    private final WppConnectionRepository connectionRepository;
    private final WppTokenService tokenService;
    private final CompanyRepository companyRepository;
    private final RestTemplate restTemplate;

    public WppConnectService(WppConnectSessionRepository sessionRepository,
            WppConnectionRepository connectionRepository,
            WppTokenService tokenService,
            CompanyRepository companyRepository) {
        this.sessionRepository = sessionRepository;
        this.connectionRepository = connectionRepository;
        this.tokenService = tokenService;
        this.companyRepository = companyRepository;
        this.restTemplate = new RestTemplate();
    }


    @Transactional
    public Map<String, String> startSession() {
        String tenantId = TenantContext.getCurrentTenant();
        String sid = UUID.randomUUID().toString();

        WppConnectSession session = new WppConnectSession();
        session.setSid(sid);
        session.setTenantId(tenantId);
        session.setStatus(WppConnectSession.SessionStatus.PENDING);
        session.setExpiresAt(LocalDateTime.now().plusMinutes(sessionTtlMinutes));

        sessionRepository.save(session);

        logger.info("Created WPP connect session: sid={}, tenant={}, expiresAt={}", sid, tenantId,
                session.getExpiresAt());

        return Map.of(
                "sid", sid,
                "configId", metaConfigId,
                "appId", metaAppId);
    }


    @Transactional
    public Map<String, Object> finishSession(String sid, String code, String wabaId, String phoneNumberId) {
        String tenantId = TenantContext.getCurrentTenant();


        WppConnectSession session = sessionRepository.findBySid(sid)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sid));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus(WppConnectSession.SessionStatus.EXPIRED);
            sessionRepository.save(session);
            throw new RuntimeException("Session expired");
        }

        if (session.getStatus() != WppConnectSession.SessionStatus.PENDING) {
            throw new RuntimeException("Session is not in PENDING state");
        }

        if (!session.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Session tenant mismatch");
        }


        String accessToken = exchangeCodeForToken(code);


        WppConnection connection = connectionRepository.findByPhoneNumberId(phoneNumberId)
                .orElse(new WppConnection());
        connection.setTenantId(tenantId);
        connection.setWabaId(wabaId);
        connection.setPhoneNumberId(phoneNumberId);
        connection.setStatus(WppConnection.WppConnectionStatus.CONNECTED);

        connectionRepository.save(connection);
        tokenService.encryptAndSave(connection, accessToken);


        connectionRepository.deactivateOtherConnections(tenantId, connection.getId());


        syncPhoneNumberToCompany(tenantId, phoneNumberId);


        registerPhoneNumber(phoneNumberId, accessToken);


        subscribeToWebhooks(wabaId, accessToken);


        session.setStatus(WppConnectSession.SessionStatus.CONNECTED);
        sessionRepository.save(session);

        logger.info("WPP connection completed: tenant={}, wabaId={}, phoneNumberId={}", tenantId, wabaId,
                phoneNumberId);

        return Map.of(
                "connected", true,
                "wabaId", wabaId,
                "phoneNumberId", phoneNumberId);
    }


    private String exchangeCodeForToken(String code) {
        String url = String.format(
                "https://graph.facebook.com/%s/oauth/access_token?client_id=%s&client_secret=%s&code=%s",
                graphVersion, metaAppId, metaAppSecret, code);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String token = (String) response.getBody().get("access_token");
                if (token == null || token.isEmpty()) {
                    throw new RuntimeException("No access_token in response: " + response.getBody());
                }
                logger.info("Successfully exchanged code for access token");
                return token;
            }
            throw new RuntimeException("Token exchange failed with status: " + response.getStatusCode());
        } catch (Exception e) {
            logger.error("Error exchanging code for token", e);
            throw new RuntimeException("Token exchange failed", e);
        }
    }


    private void registerPhoneNumber(String phoneNumberId, String accessToken) {
        String url = String.format("https://graph.facebook.com/%s/%s/register", graphVersion, phoneNumberId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> body = Map.of(
                    "messaging_product", "whatsapp",
                    "pin", "000000"
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, request, new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Phone number {} registered successfully", phoneNumberId);
            } else {
                logger.warn("Phone number registration returned: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error registering phone number {}", phoneNumberId, e);

        }
    }


    private void subscribeToWebhooks(String wabaId, String accessToken) {
        String url = String.format("https://graph.facebook.com/%s/%s/subscribed_apps", graphVersion, wabaId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, request, new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("WABA {} subscribed to webhooks successfully", wabaId);
            } else {
                logger.warn("Webhook subscription returned: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error subscribing WABA {} to webhooks", wabaId, e);

        }
    }


    private void syncPhoneNumberToCompany(String tenantSchema, String phoneNumberId) {
        Optional<Company> companyOpt = companyRepository.findByTenantSchema(tenantSchema);
        if (companyOpt.isPresent()) {
            Company company = companyOpt.get();
            company.setWppPhoneNumberId(phoneNumberId);
            companyRepository.save(company);
            logger.info("\u2713 Synced wppPhoneNumberId={} to companies table for tenant {}", phoneNumberId, tenantSchema);
        } else {
            logger.warn("\u2717 Could not sync wppPhoneNumberId \u2014 no company found for tenantSchema: {}", tenantSchema);
        }
    }


    @Scheduled(fixedRate = 300000)
    @Transactional
    public void purgeExpiredSessions() {
        sessionRepository.deleteExpired(LocalDateTime.now());
    }
}
