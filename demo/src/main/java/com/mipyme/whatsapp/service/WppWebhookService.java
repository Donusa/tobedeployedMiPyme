package com.mipyme.whatsapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipyme.company.Company;
import com.mipyme.company.CompanyRepository;
import com.mipyme.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

@Service
public class WppWebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WppWebhookService.class);

    @Value("${whatsapp.meta-app-secret:}")
    private String metaAppSecret;

    @Value("${whatsapp.skip-signature-validation:false}")
    private boolean skipSignatureValidation;

    private final CompanyRepository companyRepository;
    private final WppEventSaver wppEventSaver;
    private final ObjectMapper objectMapper;

    public WppWebhookService(CompanyRepository companyRepository,
            WppEventSaver wppEventSaver) {
        this.companyRepository = companyRepository;
        this.wppEventSaver = wppEventSaver;
        this.objectMapper = new ObjectMapper();
    }


    public boolean validateSignature(String rawBody, String signatureHeader) {
        if (skipSignatureValidation) {
            logger.warn("⚠️  SIGNATURE VALIDATION SKIPPED (whatsapp.skip-signature-validation=true) — DO NOT USE IN PRODUCTION");
            return true;
        }

        logger.info("=== WEBHOOK SIGNATURE VALIDATION ===");
        logger.info("Signature Header: {}", signatureHeader);
        logger.info("App Secret configured: {}", !metaAppSecret.isEmpty());
        logger.info("Raw Body Length: {} bytes", rawBody != null ? rawBody.length() : 0);

        if (signatureHeader == null) {
            logger.warn("✗ Signature header is NULL — if Meta is sending a signature, check that X-Hub-Signature-256 reaches the server");
            return false;
        }

        if (!signatureHeader.startsWith("sha256=")) {
            logger.warn("✗ Signature header doesn't start with 'sha256='");
            logger.warn("  Header: {}", signatureHeader);
            return false;
        }

        String expectedHash = signatureHeader.substring("sha256=".length());
        logger.info("Expected Hash: {}", expectedHash);

        if (rawBody == null) {
            logger.warn("\u2717 rawBody is null \u2014 cannot compute HMAC signature");
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    metaAppSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);

            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHash = bytesToHex(hash);
            logger.info("Computed Hash: {}", computedHash);

            boolean isEqual = MessageDigest.isEqual(
                    computedHash.getBytes(StandardCharsets.UTF_8),
                    expectedHash.getBytes(StandardCharsets.UTF_8));

            if (isEqual) {
                logger.info("✓ Signature validation PASSED");
            } else {
                logger.warn("✗ Signature validation FAILED - hashes do not match");
            }

            return isEqual;
        } catch (Exception e) {
            logger.error("✗ Error validating webhook signature", e);
            logger.error("  Exception: {}", e.getClass().getName());
            return false;
        }
    }


    public void processPayload(String rawBody) {
        logger.info("=== WEBHOOK PAYLOAD PROCESSING STARTED ===");
        try {

            String eventHash = sha256(rawBody);
            logger.info("Event Hash: {}", eventHash);


            JsonNode root = objectMapper.readTree(rawBody);
            logger.info("✓ JSON parsed successfully");

            JsonNode entries = root.path("entry");
            if (!entries.isArray()) {
                logger.warn("✗ No 'entry' array found in payload");
                return;
            }

            logger.info("Found {} entries", entries.size());
            int entryCount = 0;

            for (JsonNode entry : entries) {
                entryCount++;
                logger.info("--- Processing entry {}/{} ---", entryCount, entries.size());

                JsonNode changes = entry.path("changes");
                if (!changes.isArray()) {
                    logger.warn("  No 'changes' array in entry");
                    continue;
                }

                logger.info("  Found {} changes in entry", changes.size());
                int changeCount = 0;

                for (JsonNode change : changes) {
                    changeCount++;
                    logger.info("  -- Processing change {}/{} --", changeCount, changes.size());

                    JsonNode value = change.path("value");
                    String phoneNumberId = value.path("metadata").path("phone_number_id").asText(null);
                    logger.info("    Phone Number ID: {}", phoneNumberId);

                    if (phoneNumberId == null) {
                        logger.warn("    ✗ No phone_number_id in webhook payload");
                        continue;
                    }


                    logger.info("    Resolving tenant for phone_number_id: {}", phoneNumberId);
                    String tenantSchema = resolveTenantByPhoneNumberId(phoneNumberId);

                    if (tenantSchema == null) {
                        logger.warn("    ✗ No tenant found for phone_number_id: {}", phoneNumberId);
                        continue;
                    }

                    logger.info("    ✓ Tenant resolved: {}", tenantSchema);



                    TenantContext.setCurrentTenant(tenantSchema);
                    try {
                        wppEventSaver.saveEvent(eventHash, rawBody, value, value.path("contacts"));
                    } finally {
                        TenantContext.clear();
                    }
                }
            }

            logger.info("=== WEBHOOK PAYLOAD PROCESSING COMPLETED ===");
        } catch (Exception e) {
            logger.error("✗ Error processing webhook payload", e);
            logger.error("  Exception Type: {}", e.getClass().getName());
            logger.error("  Exception Message: {}", e.getMessage());
            logger.error("  Stack Trace: ", e);
        }
    }


    public String resolveTenantByPhoneNumberId(String phoneNumberId) {
        logger.info("    Resolving tenant by phoneNumberId: {} (fast lookup via companies table)", phoneNumberId);

        Optional<Company> company = companyRepository.findByWppPhoneNumberId(phoneNumberId);
        if (company.isPresent()) {
            String tenantSchema = company.get().getTenantSchema();
            logger.info("    ✓ Tenant resolved via companies table: {} (company: {})",
                    tenantSchema, company.get().getName());
            return tenantSchema;
        }

        logger.warn("    ✗ NO TENANT FOUND for phoneNumberId: {} — " +
                "wpp_phone_number_id not set in companies table. " +
                "Call /api/wpp/connection/setup or complete the Embedded Signup to register.",
                phoneNumberId);
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }
}
