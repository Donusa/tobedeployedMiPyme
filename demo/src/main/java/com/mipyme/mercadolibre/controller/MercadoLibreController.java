package com.mipyme.mercadolibre.controller;

import com.mipyme.mercadolibre.dto.MercadoLibreCodeRequest;
import com.mipyme.mercadolibre.dto.MercadoLibreTokenResponse;
import com.mipyme.mercadolibre.dto.MercadoLibreItemDTO;
import com.mipyme.mercadolibre.dto.MercadoLibreNotificationDTO;
import com.mipyme.mercadolibre.dto.MercadoLibreOrderSummaryDTO;
import com.mipyme.mercadolibre.dto.MercadoLibreBulkSyncRequest;
import com.mipyme.mercadolibre.dto.MercadoLibreSyncPreviewDTO;
import com.mipyme.mercadolibre.service.MercadoLibreService;
import com.mipyme.mercadolibre.model.MercadoLibreConfig;
import com.mipyme.mercadolibre.model.messaging.MercadoLibreConversation;
import com.mipyme.mercadolibre.model.messaging.MercadoLibreMessage;
import com.mipyme.stock.model.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/mercadolibre")
public class MercadoLibreController {

    private static final Logger logger = LoggerFactory.getLogger(MercadoLibreController.class);

    private final MercadoLibreService service;

    public MercadoLibreController(MercadoLibreService service) {
        this.service = service;
    }

    @GetMapping("/products/linked")
    public ResponseEntity<List<Product>> getLinkedProducts() {
        return ResponseEntity.ok(service.getLinkedLocalProducts());
    }

    @PostMapping("/products/unlink")
    public ResponseEntity<Void> unlinkProducts(@RequestBody List<Long> productIds) {
        service.unlinkProducts(productIds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/token")
    public ResponseEntity<MercadoLibreTokenResponse> exchangeToken(@RequestBody MercadoLibreCodeRequest request) {
        return ResponseEntity.ok(service.exchangeToken(request.getCode(), request.getCodeVerifier()));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        MercadoLibreConfig config = service.getConfig();
        if (config != null && config.getAccessToken() != null) {
            return ResponseEntity.ok(Map.of(
                    "connected", true,
                    "user_id", config.getUserId(),
                    "expires_at", config.getExpiresAt() != null ? config.getExpiresAt().toString() : "unknown"));
        }
        return ResponseEntity.ok(Collections.singletonMap("connected", false));
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getPublicConfig() {
        return ResponseEntity.ok(Map.of(
                "appId", service.getAppId(),
                "redirectUri", service.getRedirectUri()));
    }

    @GetMapping("/products")
    public ResponseEntity<List<MercadoLibreItemDTO>> getProducts() {
        return ResponseEntity.ok(service.getProducts());
    }

    @GetMapping("/orders")
    public ResponseEntity<List<MercadoLibreOrderSummaryDTO>> getOrders() {
        return ResponseEntity.ok(service.getOrders());
    }

    @PostMapping("/unlink")
    public ResponseEntity<Void> unlink() {
        service.unlinkAccount();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync")
    public ResponseEntity<Void> syncProducts(@RequestBody com.mipyme.mercadolibre.dto.MercadoLibreSyncRequest request) {
        service.syncProducts(request.getItemIds());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync/preview-bulk")
    public ResponseEntity<List<MercadoLibreSyncPreviewDTO>> previewBulkSync(
            @RequestBody List<MercadoLibreBulkSyncRequest> requests) {
        return ResponseEntity.ok(service.previewBulkSync(requests));
    }

    @PostMapping("/orders/{id}/local-status")
    public ResponseEntity<Void> updateLocalStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status != null) {
            service.updateLocalStatus(id, status);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync/execute")
    public ResponseEntity<Void> executeSync(@RequestBody List<MercadoLibreSyncPreviewDTO> previews) {
        service.executeSync(previews);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhooks")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody MercadoLibreNotificationDTO notification,
            @RequestHeader Map<String, String> headers,
            HttpServletRequest request) {
        logger.info("Webhook MercadoLibre recibido: topic={}, resource={}, userId={}, ip={}, headers={}",
                notification.getTopic(),
                notification.getResource(),
                notification.getUserId(),
                request != null ? request.getRemoteAddr() : "unknown",
                headers);
        logger.info("Webhook MercadoLibre payload: {}", notification.toString());
        service.handleNotification(notification);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<MercadoLibreConversation>> getConversations() {
        return ResponseEntity.ok(service.getConversations());
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<MercadoLibreMessage>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "before", required = false) String before) {
        if (limit != null) {
            return ResponseEntity.ok(service.getMessages(conversationId, limit, before));
        }
        return ResponseEntity.ok(service.getMessages(conversationId));
    }

    @PostMapping("/conversations/{conversationId}/ensure")
    public ResponseEntity<MercadoLibreConversation> ensureConversation(@PathVariable Long conversationId) {
        return ResponseEntity.ok(service.ensureConversation(conversationId));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MercadoLibreMessage> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody String messageText) {
        return ResponseEntity.ok(service.sendMessage(conversationId, messageText));
    }



    @PostMapping("/products/publish")
    public ResponseEntity<?> publishProduct(@RequestBody Map<String, Object> request) {
        try {
            Long localProductId = Long.valueOf(String.valueOf(request.get("localProductId")));
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) request.getOrDefault("fields",
                    new java.util.HashMap<>());
            Map<String, Object> result = service.publishProduct(localProductId, fields);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new java.util.HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/products/{localProductId}")
    public ResponseEntity<?> updatePublishedProduct(
            @PathVariable Long localProductId,
            @RequestBody Map<String, Object> fields) {
        try {
            Map<String, Object> result = service.updatePublishedProduct(localProductId, fields);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new java.util.HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/products/{localProductId}/published")
    public ResponseEntity<Map<String, Object>> getPublishedProduct(@PathVariable Long localProductId) {
        return ResponseEntity.ok(service.getPublishedProduct(localProductId));
    }

    @GetMapping("/categories/search")
    public ResponseEntity<java.util.List<Map<String, Object>>> searchCategories(@RequestParam String q) {
        return ResponseEntity.ok(service.searchCategories(q));
    }

    @GetMapping("/categories/{categoryId}/listing-types")
    public ResponseEntity<java.util.List<Map<String, Object>>> getListingTypes(@PathVariable String categoryId) {
        return ResponseEntity.ok(service.getListingTypes(categoryId));
    }

    @GetMapping("/categories/{categoryId}/attributes")
    public ResponseEntity<java.util.List<Map<String, Object>>> getCategoryAttributes(@PathVariable String categoryId) {
        return ResponseEntity.ok(service.getCategoryAttributes(categoryId));
    }

    @GetMapping("/categories")
    public ResponseEntity<java.util.List<Map<String, Object>>> getCategories(
            @RequestParam(required = false) String parentId) {
        return ResponseEntity.ok(service.getCategories(parentId));
    }

    @PutMapping("/items/{mlItemId}")
    public ResponseEntity<?> updateItemDirect(
            @PathVariable String mlItemId,
            @RequestBody Map<String, Object> fields) {
        try {
            Map<String, Object> result = service.updateItemDirect(mlItemId, fields);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new java.util.HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/items/{mlItemId}")
    public ResponseEntity<?> deleteItemDirect(@PathVariable String mlItemId) {
        try {
            Map<String, Object> result = service.deleteItemDirect(mlItemId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new java.util.HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
