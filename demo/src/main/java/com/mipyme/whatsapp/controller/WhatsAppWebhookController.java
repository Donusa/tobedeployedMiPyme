package com.mipyme.whatsapp.controller;

import com.mipyme.company.Company;
import com.mipyme.company.CompanyRepository;
import com.mipyme.whatsapp.model.WppConnection;
import com.mipyme.whatsapp.repository.WppConnectionRepository;
import com.mipyme.whatsapp.service.WppTokenService;
import com.mipyme.whatsapp.service.WppWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    @Value("${whatsapp.verify-token:}")
    private String verifyToken;

    @Value("${whatsapp.meta-app-secret:}")
    private String metaAppSecret;

    @Value("${whatsapp.skip-signature-validation:false}")
    private boolean skipSig;

    private final WppWebhookService webhookService;
    private final WppConnectionRepository connectionRepository;
    private final WppTokenService tokenService;
    private final CompanyRepository companyRepository;

    public WhatsAppWebhookController(WppWebhookService webhookService,
            WppConnectionRepository connectionRepository,
            WppTokenService tokenService,
            CompanyRepository companyRepository) {
        this.webhookService = webhookService;
        this.connectionRepository = connectionRepository;
        this.tokenService = tokenService;
        this.companyRepository = companyRepository;
    }


    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        logger.info("=== WhatsApp STATUS CHECK ===");
        Optional<WppConnection> connection = connectionRepository.findAll().stream()
                .filter(c -> c.getStatus() == WppConnection.WppConnectionStatus.CONNECTED)
                .findFirst();

        if (connection.isPresent()) {
            WppConnection conn = connection.get();
            logger.info("✓ WhatsApp connection is CONNECTED");
            logger.info("  Phone Number ID: {}", conn.getPhoneNumberId());
            logger.info("  WABA ID: {}", conn.getWabaId());
            logger.info("  Display Phone: {}", conn.getDisplayPhoneNumber());
            return ResponseEntity.ok(Map.of(
                    "connected", true,
                    "phoneNumberId", conn.getPhoneNumberId(),
                    "wabaId", conn.getWabaId(),
                    "displayPhoneNumber", conn.getDisplayPhoneNumber() != null ? conn.getDisplayPhoneNumber() : ""));
        }

        logger.warn("✗ No CONNECTED WhatsApp connection found");
        return ResponseEntity.ok(Map.of("connected", false));
    }


    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        logger.info("=== WhatsApp WEBHOOK VERIFICATION INITIATED ===");
        logger.info("Mode: {}", mode);
        logger.info("Challenge: {}", challenge);
        logger.info("Verify Token Received: {}", token);
        logger.info("Verify Token Expected: {}", verifyToken);
        logger.info("Mode matches 'subscribe': {}", "subscribe".equals(mode));
        logger.info("Token matches: {}", verifyToken.equals(token));

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            logger.info("✓ WhatsApp webhook VERIFIED SUCCESSFULLY. Responding with challenge.");
            return ResponseEntity.ok(challenge);
        }

        logger.warn("✗ WhatsApp webhook VERIFICATION FAILED: token mismatch or invalid mode.");
        logger.warn("  Mode check: {} (expected: 'subscribe')", mode);
        logger.warn("  Token check: {} (expected: {})", token, verifyToken);
        return ResponseEntity.status(403).body("Verification failed");
    }


    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            HttpServletRequest request) {

        logger.info("=== WhatsApp WEBHOOK POST RECEIVED ===");
        logger.info("Remote IP: {}", request.getRemoteAddr());
        logger.info("Signature Header: {}", signature);
        logger.info("Raw Body Length: {} bytes", rawBody != null ? rawBody.length() : 0);
        logger.debug("Raw Body (first 500 chars): {}",
            rawBody != null ? rawBody.substring(0, Math.min(500, rawBody.length())) : "NULL");


        logger.info("Validating webhook signature...");
        if (!webhookService.validateSignature(rawBody, signature)) {
            logger.warn("✗ WhatsApp webhook signature validation FAILED");
            logger.warn("  Signature Header: {}", signature);
            logger.warn("  Cannot process webhook without valid signature");
            return ResponseEntity.status(401).build();
        }

        logger.info("✓ Webhook signature validated OK");



        try {
            logger.info("Starting webhook payload processing...");
            webhookService.processPayload(rawBody);
            logger.info("✓ Webhook payload processed successfully");
        } catch (Exception e) {
            logger.error("✗ Error processing webhook payload", e);
            logger.error("  Exception type: {}", e.getClass().getName());
            logger.error("  Exception message: {}", e.getMessage());

        }

        logger.info("=== WhatsApp WEBHOOK POST COMPLETED ===");
        return ResponseEntity.ok().build();
    }


    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnect() {
        logger.info("=== WhatsApp DISCONNECT REQUESTED ===");

        try {
            Optional<WppConnection> connection = connectionRepository.findAll().stream()
                    .filter(c -> c.getStatus() == WppConnection.WppConnectionStatus.CONNECTED)
                    .findFirst();

            if (connection.isPresent()) {
                WppConnection conn = connection.get();
                logger.info("Found CONNECTED WhatsApp connection: ID={}, PhoneNumberId={}",
                    conn.getId(), conn.getPhoneNumberId());

                conn.setStatus(WppConnection.WppConnectionStatus.DISCONNECTED);
                conn.setAccessTokenCiphertext(null);
                conn.setAccessTokenIv(null);
                conn.setAccessTokenKeyVersion(null);
                connectionRepository.save(conn);
                logger.info("✓ Connection status updated to DISCONNECTED");

                tokenService.invalidateCache(
                        com.mipyme.tenant.TenantContext.getCurrentTenant(),
                        conn.getId());
                logger.info("✓ Token cache invalidated");
            } else {
                logger.warn("No CONNECTED WhatsApp connection found to disconnect");
            }

            logger.info("✓ WhatsApp disconnected successfully");
            return ResponseEntity.ok(Map.of("disconnected", true));
        } catch (Exception e) {
            logger.error("✗ Error disconnecting WhatsApp", e);
            logger.error("  Exception: {}", e.getClass().getName());
            logger.error("  Message: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/deauthorize")
    public ResponseEntity<Void> deauthorize(
            @RequestBody(required = false) String body,
            @RequestHeader Map<String, String> headers) {

        logger.info("=== WhatsApp DEAUTHORIZE REQUEST RECEIVED ===");
        logger.info("Headers: {}", headers);
        logger.info("Body: {}", body);
        logger.info("✓ Deauthorize request processed");

        return ResponseEntity.ok().build();
    }


    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debug() {
        Map<String, Object> result = new LinkedHashMap<>();


        List<WppConnection> connected = connectionRepository.findAll().stream()
                .filter(c -> c.getStatus() == WppConnection.WppConnectionStatus.CONNECTED)
                .collect(Collectors.toList());

        result.put("connectedConnections", connected.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("tenantId", c.getTenantId());
            m.put("phoneNumberId", c.getPhoneNumberId());
            m.put("wabaId", c.getWabaId());
            m.put("displayPhone", c.getDisplayPhoneNumber());
            m.put("hasToken", c.getAccessTokenCiphertext() != null);
            return m;
        }).collect(Collectors.toList()));


        List<Company> companies = companyRepository.findAll();
        result.put("companies", companies.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("tenantSchema", c.getTenantSchema());
            m.put("wppPhoneNumberId", c.getWppPhoneNumberId());
            return m;
        }).collect(Collectors.toList()));


        result.put("skipSignatureValidation", skipSig);
        result.put("appSecretConfigured", metaAppSecret != null && !metaAppSecret.isBlank());
        result.put("appSecretLength", metaAppSecret != null ? metaAppSecret.length() : 0);
        result.put("verifyToken", verifyToken);

        return ResponseEntity.ok(result);
    }
}
