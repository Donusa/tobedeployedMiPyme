package com.mipyme.mercadopago.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipyme.mercadopago.model.MpWebhookEvent;
import com.mipyme.mercadopago.repository.MpWebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/mercadopago")
public class MercadoPagoWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    @Value("${mercadopago.webhook.secret:}")
    private String webhookSecret;

    private static final Pattern TS_PATTERN = Pattern.compile("ts=(\\w+)");
    private static final Pattern V1_PATTERN = Pattern.compile("v1=([a-fA-F0-9]+)");

    private final ObjectMapper objectMapper;
    private final MpWebhookEventRepository eventRepository;
    private final com.mipyme.mercadopago.service.MpWebhookProcessor webhookProcessor;

    public MercadoPagoWebhookController(
            ObjectMapper objectMapper,
            MpWebhookEventRepository eventRepository,
            com.mipyme.mercadopago.service.MpWebhookProcessor webhookProcessor) {
        this.objectMapper = objectMapper;
        this.eventRepository = eventRepository;
        this.webhookProcessor = webhookProcessor;
    }


    @PostMapping("/webhooks")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody,
            @RequestParam(value = "id", required = false) String queryId,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "data.id", required = false) String paramDataId,
            @RequestHeader HttpHeaders headers,
            HttpServletRequest request) {

        logger.info("=== MercadoPago Webhook Received ===");
        logger.info("Remote IP: {}", request.getRemoteAddr());
        logger.info("Query params -> id: {}, topic: {}, data.id: {}", queryId, topic, paramDataId);
        logger.info("Headers: {}", headers);
        logger.info("Body: {}", rawBody);

        String type = "unknown";
        String action = "unknown";
        String dataIdStr = paramDataId;
        String eventId = null;

        try {
            JsonNode rootNode = objectMapper.readTree(rawBody);

            if (rootNode.has("type") && !rootNode.get("type").isNull()) {
                type = rootNode.get("type").asText();
            } else if (topic != null) {
                type = topic;
            }

            if (rootNode.has("action") && !rootNode.get("action").isNull()) {
                action = rootNode.get("action").asText();
            }

            if (rootNode.has("id") && !rootNode.get("id").isNull()) {
                eventId = rootNode.get("id").asText();
            }


            if (rootNode.has("data") && rootNode.get("data").has("id") && !rootNode.get("data").get("id").isNull()) {
                dataIdStr = rootNode.get("data").get("id").asText();
            }

        } catch (Exception e) {
            logger.warn("Could not parse webhook body as JSON", e);
        }


        boolean signatureValid = validateSignature(headers, dataIdStr);
        if (!signatureValid) {
            logger.warn("⚠️ POSIBLE REQUEST FRAUDULENTA - La firma HMAC no coincide con el secret configurado");
            logger.info("=== End MercadoPago Webhook ===");
            return ResponseEntity.status(403).build();
        }

        logger.info("✅ Firma HMAC validada correctamente");

        String xSignature = headers.getFirst("x-signature");
        String ts = "0";
        String v1 = "";
        if (xSignature != null) {
            Matcher tsMatcher = TS_PATTERN.matcher(xSignature);
            Matcher v1Matcher = V1_PATTERN.matcher(xSignature);
            if (tsMatcher.find())
                ts = tsMatcher.group(1);
            if (v1Matcher.find())
                v1 = v1Matcher.group(1);
        }

        MpWebhookEvent event = new MpWebhookEvent();
        event.setType(type);
        event.setAction(action);
        event.setDataId(dataIdStr != null ? dataIdStr : "");
        event.setEventId(eventId);
        event.setxRequestId(headers.getFirst("x-request-id"));
        event.setTs(ts);
        event.setV1(v1);
        event.setRawBody(rawBody);
        event.setStatus(MpWebhookEvent.WebhookStatus.RECEIVED);
        event.setReceivedAt(LocalDateTime.now());
        event.setAttempts(0);

        try {
            eventRepository.save(event);
            logger.info("Webhook event persisted with ID: {}", event.getId());


            webhookProcessor.processEventAsync(event.getId());

        } catch (Exception e) {
            logger.error("Error saving webhook event to database. Duplicate event or transient error.", e);
        }

        logger.info("=== End MercadoPago Webhook ===");

        return ResponseEntity.ok().build();
    }


    private boolean validateSignature(HttpHeaders headers, String dataId) {
        String xSignature = headers.getFirst("x-signature");
        String xRequestId = headers.getFirst("x-request-id");

        if (xSignature == null || xSignature.isBlank()) {
            logger.warn("⚠️ Header x-signature ausente en la request");
            return false;
        }


        Matcher tsMatcher = TS_PATTERN.matcher(xSignature);
        Matcher v1Matcher = V1_PATTERN.matcher(xSignature);

        if (!tsMatcher.find() || !v1Matcher.find()) {
            logger.warn("⚠️ Formato inválido del header x-signature: {}", xSignature);
            return false;
        }

        String ts = tsMatcher.group(1);
        String v1 = v1Matcher.group(1);


        StringBuilder manifest = new StringBuilder();
        manifest.append("id:").append(dataId != null ? dataId : "").append(";");
        manifest.append("request-id:").append(xRequestId != null ? xRequestId : "").append(";");
        manifest.append("ts:").append(ts).append(";");

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(manifest.toString().getBytes(StandardCharsets.UTF_8));

            String computedHmac = bytesToHex(hash);

            if (computedHmac.equalsIgnoreCase(v1)) {
                return true;
            } else {
                logger.warn("⚠️ HMAC mismatch — esperado: {}, recibido: {}", computedHmac, v1);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error calculando HMAC", e);
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
