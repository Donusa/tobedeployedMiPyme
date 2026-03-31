package com.mipyme.whatsapp.service;

import com.mipyme.company.Company;
import com.mipyme.company.CompanyRepository;
import com.mipyme.tenant.TenantContext;
import com.mipyme.whatsapp.model.WppConnection;
import com.mipyme.whatsapp.model.WppConversation;
import com.mipyme.whatsapp.model.WppMessage;
import com.mipyme.whatsapp.repository.WppConnectionRepository;
import com.mipyme.whatsapp.repository.WppConversationRepository;
import com.mipyme.whatsapp.repository.WppMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WppApiService {

    private static final Logger logger = LoggerFactory.getLogger(WppApiService.class);

    private final WppConversationRepository conversationRepository;
    private final WppMessageRepository messageRepository;
    private final WppConnectionRepository connectionRepository;
    private final WppTokenService tokenService;
    private final WppCloudApiClient cloudApiClient;
    private final CompanyRepository companyRepository;

    public WppApiService(WppConversationRepository conversationRepository,
            WppMessageRepository messageRepository,
            WppConnectionRepository connectionRepository,
            WppTokenService tokenService,
            WppCloudApiClient cloudApiClient,
            CompanyRepository companyRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.connectionRepository = connectionRepository;
        this.tokenService = tokenService;
        this.cloudApiClient = cloudApiClient;
        this.companyRepository = companyRepository;
    }

    public List<WppConversation> getConversations() {
        return conversationRepository.findAllByOrderByLastMessageAtDesc();
    }

    public List<WppMessage> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
    }

    @Transactional
    public WppMessage sendMessage(String contactWaId, String text) {
        String tenantId = TenantContext.getCurrentTenant();


        WppConnection connection = connectionRepository
                .findFirstByTenantIdAndStatusOrderByIdDesc(
                        tenantId, WppConnection.WppConnectionStatus.CONNECTED)
                .orElseThrow(() -> new RuntimeException(
                        "No active WhatsApp connection for tenant: " + tenantId));

        String accessToken = tokenService.getDecryptedToken(connection);
        if (accessToken == null || accessToken.isBlank()) {
            throw new RuntimeException("Cannot decrypt WhatsApp access token — token is null or empty");
        }
        logger.info("Using access token: length={}, prefix={}...",
                accessToken.length(), accessToken.substring(0, Math.min(10, accessToken.length())));


        String wamid;
        boolean failed = false;
        String errorMessage = null;

        try {
            wamid = cloudApiClient.sendTextMessage(
                    connection.getPhoneNumberId(), accessToken, contactWaId, text);
        } catch (Exception e) {
            failed = true;
            errorMessage = e.getMessage();
            wamid = "failed_" + System.currentTimeMillis();
            logger.error("Failed to send message to {}: {}", contactWaId, errorMessage);
        }


        WppConversation conversation = conversationRepository.findByContactWaId(contactWaId)
                .orElseGet(() -> {
                    WppConversation c = new WppConversation();
                    c.setContactWaId(contactWaId);
                    return c;
                });
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);


        WppMessage message = new WppMessage();
        message.setWamid(wamid);
        message.setConversation(conversation);
        message.setDirection(WppMessage.MessageDirection.OUT);
        message.setType("text");
        message.setTextBody(text);
        message.setTimestamp(LocalDateTime.now());

        if (failed) {
            message.setRawPayload("FAILED: " + errorMessage);
        }

        messageRepository.save(message);

        return message;
    }

    @Transactional
    public void markConversationRead(Long conversationId) {
        conversationRepository.findById(conversationId).ifPresent(conv -> {
            conv.setUnreadCount(0);
            conversationRepository.save(conv);
        });
    }

    @Transactional
    public WppConnection setupConnection(String wabaId, String phoneNumberId, String displayPhoneNumber, String accessToken) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new RuntimeException("Tenant context is required");
        }

        if (wabaId == null || wabaId.isBlank() ||
            phoneNumberId == null || phoneNumberId.isBlank() ||
            accessToken == null || accessToken.isBlank()) {
            throw new RuntimeException("Missing required fields: wabaId, phoneNumberId, accessToken");
        }


        WppConnection connection = connectionRepository.findByPhoneNumberId(phoneNumberId)
                .orElse(new WppConnection());
        connection.setTenantId(tenantId);
        connection.setWabaId(wabaId);
        connection.setPhoneNumberId(phoneNumberId);
        connection.setDisplayPhoneNumber(displayPhoneNumber);
        connection.setStatus(WppConnection.WppConnectionStatus.CONNECTED);


        connection = connectionRepository.save(connection);


        tokenService.encryptAndSave(connection, accessToken);


        connectionRepository.deactivateOtherConnections(tenantId, connection.getId());

        logger.info("WhatsApp connection setup (upsert) — tenant: {}, WABA ID: {}, phoneNumberId: {}, id: {}",
                   tenantId, wabaId, phoneNumberId, connection.getId());


        syncPhoneNumberToCompany(tenantId, phoneNumberId);

        return connection;
    }


    public List<Map<String, Object>> syncHistory(int previewSize) {
        List<WppConversation> convs = conversationRepository.findAllByOrderByLastMessageAtDesc();
        List<Map<String, Object>> result = new ArrayList<>(convs.size());

        for (WppConversation conv : convs) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id",             conv.getId());
            entry.put("contactWaId",    conv.getContactWaId());
            entry.put("contactName",    conv.getContactName());
            entry.put("lastMessageAt",  conv.getLastMessageAt());
            entry.put("unreadCount",    conv.getUnreadCount());
            entry.put("createdAt",      conv.getCreatedAt());


            List<WppMessage> msgs = messageRepository.findByConversationIdOrderByTimestampAsc(conv.getId());
            entry.put("recentMessages", msgs);

            result.add(entry);
        }

        logger.info("syncHistory: returned {} conversations", result.size());
        return result;
    }


    private void syncPhoneNumberToCompany(String tenantSchema, String phoneNumberId) {
        Optional<Company> companyOpt = companyRepository.findByTenantSchema(tenantSchema);
        if (companyOpt.isPresent()) {
            Company company = companyOpt.get();
            company.setWppPhoneNumberId(phoneNumberId);
            companyRepository.save(company);
            logger.info("✓ Synced wppPhoneNumberId={} to companies table for tenant {}", phoneNumberId, tenantSchema);
        } else {
            logger.warn("✗ Could not sync wppPhoneNumberId — no company found for tenantSchema: {}", tenantSchema);
        }
    }
}
