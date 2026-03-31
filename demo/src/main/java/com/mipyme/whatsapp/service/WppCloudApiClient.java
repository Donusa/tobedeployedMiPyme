package com.mipyme.whatsapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class WppCloudApiClient {

    private static final Logger logger = LoggerFactory.getLogger(WppCloudApiClient.class);

    @Value("${whatsapp.graph-version:v18.0}")
    private String graphVersion;

    private final RestTemplate restTemplate;

    public WppCloudApiClient() {
        this.restTemplate = new RestTemplate();
    }


    public String sendTextMessage(String phoneNumberId, String accessToken, String to, String text) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new RuntimeException("WhatsApp access token is null or empty — re-configure the connection");
        }


        String normalizedTo = normalizePhoneNumber(to);
        if (!normalizedTo.equals(to)) {
            logger.info("Phone number normalized: {} → {}", to, normalizedTo);
        }
        String url = String.format("https://graph.facebook.com/%s/%s/messages", graphVersion, phoneNumberId);
        logger.debug("Calling Cloud API: url={}, token_length={}", url, accessToken.length());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", normalizedTo,
                "type", "text",
                "text", Map.of("body", text));

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, request, new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                var messages = (java.util.List<Map<String, String>>) response.getBody().get("messages");
                if (messages != null && !messages.isEmpty()) {
                    String wamid = messages.get(0).get("id");
                    logger.info("Message sent successfully: wamid={}, to={}", wamid, to);
                    return wamid;
                }
            }

            logger.warn("Unexpected response from Cloud API: {}", response.getBody());
            return null;

        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            logger.error("Cloud API error sending message to {}: {} - {}", to, e.getStatusCode(), responseBody);
            if (e.getStatusCode().value() == 401) {
                logger.error("401 Unauthorized — the stored WhatsApp access token is invalid or expired. "
                        + "Use POST /api/wpp/connection/setup to reconfigure with a valid System User token.");
            }
            throw new RuntimeException("WhatsApp send failed [" + e.getStatusCode() + "]: "
                    + (responseBody.isBlank() ? "empty response body – likely invalid/expired token" : responseBody), e);
        } catch (Exception e) {
            logger.error("Error sending WhatsApp message to {}", to, e);
            throw new RuntimeException("WhatsApp send failed", e);
        }
    }


    static String normalizePhoneNumber(String phone) {
        if (phone == null) return null;

        String digits = phone.startsWith("+") ? phone.substring(1) : phone;

        if (digits.startsWith("549") && digits.length() == 13) {
            return "54" + digits.substring(3);
        }
        return digits;
    }


    public String getMediaUrl(String mediaId, String accessToken) {
        String url = String.format("https://graph.facebook.com/%s/%s", graphVersion, mediaId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, request, new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("url");
            }
            return null;
        } catch (Exception e) {
            logger.error("Error getting media URL for mediaId={}", mediaId, e);
            return null;
        }
    }


    public byte[] downloadMedia(String mediaUrl, String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(mediaUrl, HttpMethod.GET, request, byte[].class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            logger.error("Error downloading media from {}", mediaUrl, e);
            return null;
        }
    }
}
