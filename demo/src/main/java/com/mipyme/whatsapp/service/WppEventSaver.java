package com.mipyme.whatsapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mipyme.whatsapp.model.*;
import com.mipyme.whatsapp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;


@Service
public class WppEventSaver {

    private static final Logger logger = LoggerFactory.getLogger(WppEventSaver.class);

    private final WppWebhookLogRepository webhookLogRepository;
    private final WppConversationRepository conversationRepository;
    private final WppMessageRepository messageRepository;
    private final WppStatusRepository statusRepository;

    public WppEventSaver(WppWebhookLogRepository webhookLogRepository,
            WppConversationRepository conversationRepository,
            WppMessageRepository messageRepository,
            WppStatusRepository statusRepository) {
        this.webhookLogRepository = webhookLogRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.statusRepository = statusRepository;
    }


    @Transactional
    public void saveEvent(String eventHash, String rawBody, JsonNode value, JsonNode contacts) {

        logger.info("    Checking for duplicate event...");
        if (webhookLogRepository.existsByEventHash(eventHash)) {
            logger.warn("    ✗ Duplicate webhook event, skipping: {}", eventHash);
            return;
        }

        logger.info("    ✓ Event is not a duplicate, saving to webhook log");
        WppWebhookLog log = new WppWebhookLog();
        log.setEventHash(eventHash);
        log.setPayload(rawBody);
        log.setProcessed(false);
        webhookLogRepository.save(log);
        logger.info("    ✓ Webhook log saved with ID: {}", log.getId());


        JsonNode messages = value.path("messages");
        if (messages.isArray()) {
            logger.info("    Processing {} messages", messages.size());
            for (JsonNode msg : messages) {
                processIncomingMessage(msg, contacts);
            }
        } else {
            logger.debug("    No messages in change");
        }


        JsonNode statuses = value.path("statuses");
        if (statuses.isArray()) {
            logger.info("    Processing {} statuses", statuses.size());
            for (JsonNode status : statuses) {
                processStatusUpdate(status);
            }
        } else {
            logger.debug("    No statuses in change");
        }


        logger.info("    Marking webhook as processed");
        log.setProcessed(true);
        webhookLogRepository.save(log);
        logger.info("    ✓ Webhook marked as processed");
    }





    private void processIncomingMessage(JsonNode msg, JsonNode contacts) {
        logger.info("    === PROCESSING INCOMING MESSAGE ===");

        String wamid = msg.path("id").asText(null);
        logger.info("    Message WAMID: {}", wamid);

        if (wamid == null) {
            logger.warn("    ✗ No message ID found");
            return;
        }


        logger.info("    Checking for duplicate message (WAMID: {})...", wamid);
        if (messageRepository.existsByWamid(wamid)) {
            logger.warn("    ✗ Duplicate message wamid={}, skipping", wamid);
            return;
        }
        logger.info("    ✓ Message is not a duplicate");

        String from = msg.path("from").asText("");
        String type = msg.path("type").asText("text");
        long ts = msg.path("timestamp").asLong(0);
        LocalDateTime timestamp = ts > 0
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneId.systemDefault())
                : LocalDateTime.now();

        logger.info("    From: {}", from);
        logger.info("    Type: {}", type);
        logger.info("    Timestamp: {}", timestamp);


        logger.info("    Looking up or creating conversation for contact: {}", from);
        WppConversation conversation = conversationRepository.findByContactWaId(from)
                .orElseGet(() -> {
                    logger.info("    ℹ Creating new conversation for contact: {}", from);
                    WppConversation c = new WppConversation();
                    c.setContactWaId(from);
                    return c;
                });


        if (contacts != null && contacts.isArray()) {
            logger.info("    Looking for contact name in {} contacts...", contacts.size());
            for (JsonNode contact : contacts) {
                if (from.equals(contact.path("wa_id").asText())) {
                    String name = contact.path("profile").path("name").asText(null);
                    if (name != null) {
                        logger.info("    ✓ Contact name found: {}", name);
                        conversation.setContactName(name);
                    }
                    break;
                }
            }
        } else {
            logger.debug("    No contacts provided in payload");
        }

        conversation.setLastMessageAt(timestamp);
        conversation.setUnreadCount(conversation.getUnreadCount() + 1);
        logger.info("    Saving conversation | Unread Count: {}", conversation.getUnreadCount());
        conversationRepository.save(conversation);
        logger.info("    ✓ Conversation saved with ID: {}", conversation.getId());


        WppMessage message = new WppMessage();
        message.setWamid(wamid);
        message.setConversation(conversation);
        message.setDirection(WppMessage.MessageDirection.IN);
        message.setType(type);
        message.setTimestamp(timestamp);

        if ("text".equals(type)) {
            String textBody = msg.path("text").path("body").asText("");
            message.setTextBody(textBody);
            logger.info("    Text Body: {}", textBody);
        }


        if (msg.has(type) && msg.path(type).has("id")) {
            String mediaId = msg.path(type).path("id").asText(null);
            String mimeType = msg.path(type).path("mime_type").asText(null);
            message.setMediaId(mediaId);
            message.setMediaMimeType(mimeType);
            logger.info("    Media ID: {}", mediaId);
            logger.info("    Media MIME Type: {}", mimeType);
        }

        message.setRawPayload(msg.toString());
        messageRepository.save(message);

        logger.info("    ✓ SAVED incoming message - ID: {}, WAMID: {}, From: {}, Type: {}",
                message.getId(), wamid, from, type);
    }

    private void processStatusUpdate(JsonNode statusNode) {
        logger.info("    === PROCESSING STATUS UPDATE ===");

        String wamid = statusNode.path("id").asText(null);
        String statusStr = statusNode.path("status").asText(null);
        long ts = statusNode.path("timestamp").asLong(0);

        logger.info("    Message WAMID: {}", wamid);
        logger.info("    Status: {}", statusStr);

        if (wamid == null || statusStr == null) {
            logger.warn("    ✗ Missing wamid or status in update");
            return;
        }

        LocalDateTime timestamp = ts > 0
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneId.systemDefault())
                : LocalDateTime.now();

        logger.info("    Timestamp: {}", timestamp);


        logger.info("    Checking for duplicate status...");
        if (statusRepository.existsByWamidAndStatusAndTimestamp(wamid, statusStr, timestamp)) {
            logger.warn("    ✗ Duplicate status wamid={}, status={}, skipping", wamid, statusStr);
            return;
        }

        WppStatus status = new WppStatus();
        status.setWamid(wamid);
        status.setStatus(statusStr);
        status.setTimestamp(timestamp);


        JsonNode errors = statusNode.path("errors");
        if (errors.isArray() && errors.size() > 0) {
            JsonNode firstError = errors.get(0);
            String errorCode = firstError.path("code").asText(null);
            String errorMessage = firstError.path("title").asText(null);
            status.setErrorCode(errorCode);
            status.setErrorMessage(errorMessage);
            logger.warn("    ✗ Error in status update - Code: {}, Message: {}", errorCode, errorMessage);
        }

        statusRepository.save(status);
        logger.info("    ✓ SAVED status update - WAMID: {}, Status: {}", wamid, statusStr);
    }
}
