package com.mipyme.arca.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.mipyme.arca.model.ArcaConfig;
import com.mipyme.arca.repository.ArcaConfigRepository;
import org.springframework.transaction.annotation.Transactional;
import com.mipyme.tenant.TenantContext;

@Service
public class TaSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(TaSecurityService.class);
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int KEY_VERSION = 1;


    private final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    private final ArcaConfigRepository arcaConfigRepository;

    private byte[] masterKey;

    public TaSecurityService(ArcaConfigRepository arcaConfigRepository) {
        this.arcaConfigRepository = arcaConfigRepository;
        initializeMasterKey();
    }

    private void initializeMasterKey() {
        String envKey = System.getenv("ARCA_MASTER_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            this.masterKey = Base64.getDecoder().decode(envKey);
        } else {
            logger.error("CRITICAL: ARCA_MASTER_KEY not found in environment. Using INSECURE fallback for development only.");

            this.masterKey = "12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8);
        }

        if (this.masterKey.length != 32) {
             throw new RuntimeException("Master Key must be exactly 32 bytes (256 bits)");
        }
    }

    @Transactional
    public void encryptAndSave(ArcaConfig config, String token, String sign, LocalDateTime expiration) {
        try {

            String payload = token + "\n" + sign;
            byte[] plaintext = payload.getBytes(StandardCharsets.UTF_8);


            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);


            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            SecretKey key = new SecretKeySpec(masterKey, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);


            String tenantId = TenantContext.getCurrentTenant();
            if (tenantId == null) tenantId = "default";

            String aad = tenantId + "|" + (config.getId() != null ? config.getId() : "0") + "|" + expiration.toString();
            cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));

            byte[] ciphertext = cipher.doFinal(plaintext);


            config.setTaCiphertext(Base64.getEncoder().encodeToString(ciphertext));
            config.setTaIv(Base64.getEncoder().encodeToString(iv));
            config.setTaKeyVersion(KEY_VERSION);
            config.setTokenExpiration(expiration);

            arcaConfigRepository.save(config);


            if (config.getId() != null) {
                String cacheKey = tenantId + ":" + config.getId();
                tokenCache.put(cacheKey, new CachedToken(token, sign, expiration));
            }

        } catch (Exception e) {
            logger.error("Error encrypting TA", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public CachedToken getDecryptedToken(ArcaConfig config) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) tenantId = "default";


        Long configId = config.getId();
        String cacheKey = tenantId + ":" + configId;

        if (configId != null && tokenCache.containsKey(cacheKey)) {
            CachedToken cached = tokenCache.get(cacheKey);
            if (cached.expiration.isAfter(LocalDateTime.now())) {
                return cached;
            } else {
                tokenCache.remove(cacheKey);
            }
        }


        if (config.getTaCiphertext() == null || config.getTaIv() == null) {
            return null;
        }

        try {
            byte[] ciphertext = Base64.getDecoder().decode(config.getTaCiphertext());
            byte[] iv = Base64.getDecoder().decode(config.getTaIv());

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            SecretKey key = new SecretKeySpec(masterKey, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            String aad = tenantId + "|" + config.getId() + "|" + config.getTokenExpiration().toString();
            cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));

            byte[] plaintext = cipher.doFinal(ciphertext);
            String payload = new String(plaintext, StandardCharsets.UTF_8);

            String[] parts = payload.split("\n");
            if (parts.length != 2) throw new IllegalStateException("Invalid payload format");

            CachedToken tokenData = new CachedToken(parts[0], parts[1], config.getTokenExpiration());


            if (configId != null) {
                tokenCache.put(cacheKey, tokenData);
            }

            return tokenData;

        } catch (Exception e) {
            logger.error("Error decrypting TA", e);
            return null;
        }
    }

    @Scheduled(fixedRate = 600000)
    public void purgeExpiredTokens() {
        logger.info("Running secure purge job...");

        tokenCache.entrySet().removeIf(entry -> entry.getValue().expiration.isBefore(LocalDateTime.now()));


        arcaConfigRepository.purgeExpiredTokens(LocalDateTime.now());
    }

    public static class CachedToken {
        public String token;
        public String sign;
        public LocalDateTime expiration;

        public CachedToken(String token, String sign, LocalDateTime expiration) {
            this.token = token;
            this.sign = sign;
            this.expiration = expiration;
        }
    }
}
