package com.mipyme.whatsapp.service;

import com.mipyme.tenant.TenantContext;
import com.mipyme.whatsapp.model.WppConnection;
import com.mipyme.whatsapp.repository.WppConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class WppTokenService {

    private static final Logger logger = LoggerFactory.getLogger(WppTokenService.class);
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int KEY_VERSION = 1;

    private final Map<String, String> tokenCache = new ConcurrentHashMap<>();
    private final WppConnectionRepository connectionRepository;
    private byte[] masterKey;

    public WppTokenService(WppConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
        initializeMasterKey();
    }

    private void initializeMasterKey() {
        String envKey = System.getenv("WPP_MASTER_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            this.masterKey = Base64.getDecoder().decode(envKey);
        } else {
            logger.error(
                    "CRITICAL: WPP_MASTER_KEY not found in environment. Using INSECURE fallback for development only.");
            this.masterKey = "12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8);
        }

        if (this.masterKey.length != 32) {
            throw new RuntimeException("WPP_MASTER_KEY must be exactly 32 bytes (256 bits)");
        }
    }

    @Transactional
    public void encryptAndSave(WppConnection connection, String accessToken) {
        try {
            byte[] plaintext = accessToken.getBytes(StandardCharsets.UTF_8);

            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            SecretKey key = new SecretKeySpec(masterKey, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            String tenantId = TenantContext.getCurrentTenant();
            if (tenantId == null)
                tenantId = "default";

            String aad = tenantId + "|" + connection.getPhoneNumberId();
            cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));

            byte[] ciphertext = cipher.doFinal(plaintext);

            connection.setAccessTokenCiphertext(Base64.getEncoder().encodeToString(ciphertext));
            connection.setAccessTokenIv(Base64.getEncoder().encodeToString(iv));
            connection.setAccessTokenKeyVersion(KEY_VERSION);
            connection.setTokenCreatedAt(LocalDateTime.now());

            connectionRepository.save(connection);


            String cacheKey = tenantId + ":" + connection.getId();
            tokenCache.put(cacheKey, accessToken);

        } catch (Exception e) {
            logger.error("Error encrypting WhatsApp access token", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String getDecryptedToken(WppConnection connection) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null)
            tenantId = "default";


        String cacheKey = tenantId + ":" + connection.getId();
        if (tokenCache.containsKey(cacheKey)) {
            return tokenCache.get(cacheKey);
        }


        if (connection.getAccessTokenCiphertext() == null || connection.getAccessTokenIv() == null) {
            return null;
        }

        try {
            byte[] ciphertext = Base64.getDecoder().decode(connection.getAccessTokenCiphertext());
            byte[] iv = Base64.getDecoder().decode(connection.getAccessTokenIv());

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            SecretKey key = new SecretKeySpec(masterKey, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            String aad = tenantId + "|" + connection.getPhoneNumberId();
            cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));

            byte[] plaintext = cipher.doFinal(ciphertext);
            String token = new String(plaintext, StandardCharsets.UTF_8);

            tokenCache.put(cacheKey, token);
            return token;

        } catch (Exception e) {
            logger.error("Error decrypting WhatsApp access token", e);
            return null;
        }
    }

    public void invalidateCache(String tenantId, Long connectionId) {
        tokenCache.remove(tenantId + ":" + connectionId);
    }
}
