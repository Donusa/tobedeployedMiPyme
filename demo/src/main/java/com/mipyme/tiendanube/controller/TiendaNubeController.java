package com.mipyme.tiendanube.controller;

import com.mipyme.tiendanube.dto.CodeRequest;
import com.mipyme.tiendanube.dto.TiendaNubeProduct;
import com.mipyme.tiendanube.dto.TokenResponse;
import com.mipyme.tiendanube.dto.TiendaNubeOrderSummaryDTO;
import com.mipyme.tiendanube.service.TiendaNubeService;
import com.mipyme.tiendanube.model.TiendaNubeConfig;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import org.springframework.http.ResponseEntity;
import com.mipyme.tiendanube.dto.SyncPreviewDTO;
import com.mipyme.notification.NotificationService;
import com.mipyme.notification.model.Notification.NotificationType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tiendanube")
public class TiendaNubeController {

    private final TiendaNubeService tiendaNubeService;
    private final NotificationService notificationService;

    public TiendaNubeController(TiendaNubeService tiendaNubeService, NotificationService notificationService) {
        this.tiendaNubeService = tiendaNubeService;
        this.notificationService = notificationService;
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TiendaNubeController.class);

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> exchangeToken(@RequestBody CodeRequest request) {
        logger.info("Received /token request. Code: " + (request.getCode() != null ? "PRESENT" : "NULL")
                + " RedirectURI: " + request.getRedirectUri());
        TokenResponse response = tiendaNubeService.exchangeToken(request.getCode(), request.getRedirectUri());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync/preview")
    public ResponseEntity<List<SyncPreviewDTO>> previewSync(@RequestBody List<Long> productIds) {
        List<SyncPreviewDTO> previews = tiendaNubeService.previewSync(productIds);
        return ResponseEntity.ok(previews);
    }

    @PostMapping("/sync/preview-manual")
    public ResponseEntity<SyncPreviewDTO> previewManualMatch(@RequestBody Map<String, Long> request) {
        Long tnProductId = request.get("tnProductId");
        Long localProductId = request.get("localProductId");

        if (tnProductId == null || localProductId == null) {
            return ResponseEntity.badRequest().build();
        }

        SyncPreviewDTO preview = tiendaNubeService.previewManualMatch(tnProductId, localProductId);
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/unlink")
    public ResponseEntity<Void> unlink() {
        tiendaNubeService.unlinkStore();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync/preview-bulk")
    public List<SyncPreviewDTO> previewBulkSync(
            @RequestBody List<com.mipyme.tiendanube.dto.ManualSyncRequest> requests) {
        return tiendaNubeService.previewBulkSync(requests);
    }

    @PostMapping("/sync/execute")
    public ResponseEntity<Void> executeSync(@RequestBody List<SyncPreviewDTO> resolvedPreviews) {
        tiendaNubeService.executeSync(resolvedPreviews);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/orders")
    public ResponseEntity<List<TiendaNubeOrderSummaryDTO>> getOrders() {
        return ResponseEntity.ok(tiendaNubeService.getOrders());
    }

    @PostMapping("/webhooks/{event}")
    public ResponseEntity<Void> handleWebhook(@PathVariable String event, @RequestBody Map<String, Object> payload) {
        logger.info("Received TiendaNube webhook: {} payload: {}", event, payload);
        try {
            Object idObj = payload.get("id");
            String payloadId = idObj != null ? String.valueOf(idObj) : String.valueOf(System.currentTimeMillis());
            String refKey = "tn-" + event + "-" + payloadId;

            String message;
            String url = "/tiendanube";
            if (event.contains("order")) {
                message = "Nueva orden en TiendaNube #" + payloadId;
            } else if (event.contains("product")) {
                message = "Actualización de producto en TiendaNube";
            } else {
                message = "Notificación de TiendaNube: " + event;
            }

            notificationService.createIfNotExists(
                    NotificationType.TN_WEBHOOK, message, url, refKey);
        } catch (Exception e) {
            logger.error("Error processing TiendaNube webhook: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        TiendaNubeConfig config = tiendaNubeService.getConfig();
        if (config != null && config.getAccessToken() != null) {
            return ResponseEntity.ok(Map.of(
                    "connected", true,
                    "user_id", config.getUserId(),
                    "access_token", config.getAccessToken()
            ));
        }
        return ResponseEntity.ok(Collections.singletonMap("connected", false));
    }

    @GetMapping("/products/linked")
    public ResponseEntity<List<com.mipyme.stock.model.Product>> getLinkedProducts() {
        return ResponseEntity.ok(tiendaNubeService.getLinkedLocalProducts());
    }

    @PostMapping("/products/unlink")
    public ResponseEntity<Void> unlinkProducts(@RequestBody List<Long> productIds) {
        tiendaNubeService.unlinkProducts(productIds);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{storeId}/products")
    public ResponseEntity<List<TiendaNubeProduct>> getProducts(
            @PathVariable Long storeId,
            @RequestHeader(value = "X-TiendaNube-Access-Token", required = false) String tnAccessToken,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String accessToken = tnAccessToken;


        if (accessToken == null && authHeader != null) {
            String candidate = authHeader;
            if (candidate.toLowerCase().startsWith("bearer ")) {
                candidate = candidate.substring(7).trim();
            }



            if (!candidate.startsWith("eyJ")) {
                accessToken = candidate;
            }
        }


        if ("undefined".equals(accessToken) || "null".equals(accessToken)
                || (accessToken != null && accessToken.isEmpty())) {
            accessToken = null;
        }

        System.out.println("TiendaNube Controller - StoreID: " + storeId);
        System.out.println("TiendaNube Controller - Explicit Header: " + tnAccessToken);
        System.out.println("TiendaNube Controller - Final Token to Service: "
                + (accessToken != null && accessToken.length() > 5 ? accessToken.substring(0, 5) + "..."
                        : "null (will use DB)"));

        List<TiendaNubeProduct> products = tiendaNubeService.getProducts(storeId, accessToken);
        System.out.println(
                "TiendaNube Controller - Products retrieved: " + (products != null ? products.size() : "null"));
        return ResponseEntity.ok(products);
    }

    @GetMapping("/orders/{id}/fulfillment-orders")
    public ResponseEntity<List<Map<String, Object>>> getFulfillmentOrders(@PathVariable Long id) {
        return ResponseEntity.ok(tiendaNubeService.getFulfillmentOrders(id));
    }

    @PatchMapping("/orders/{id}/fulfillment-orders/{fulfillmentId}")
    public ResponseEntity<Map<String, Object>> updateFulfillmentOrder(
            @PathVariable Long id,
            @PathVariable String fulfillmentId,
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(tiendaNubeService.updateFulfillmentOrder(id, fulfillmentId, data));
    }

    @PostMapping("/orders/{id}/close")
    public ResponseEntity<Void> closeOrder(@PathVariable Long id) {
        tiendaNubeService.closeOrder(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders/{id}/local-status")
    public ResponseEntity<Void> updateLocalStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status != null) {
            tiendaNubeService.updateLocalTracking(String.valueOf(id), status);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders/{id}/mark-paid")
    public ResponseEntity<Void> markOrderAsPaid(@PathVariable Long id) {
        System.out.println("Processing markOrderAsPaid for order ID: " + id);
        try {
            tiendaNubeService.markOrderAsPaid(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Error marking order " + id + " as paid in controller: ");
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }



    @PostMapping("/products/publish")
    public ResponseEntity<Map<String, Object>> publishProduct(@RequestBody Map<String, Object> request) {
        Long localProductId = Long.valueOf(String.valueOf(request.get("localProductId")));
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) request.getOrDefault("fields", new java.util.HashMap<>());
        Map<String, Object> result = tiendaNubeService.publishProduct(localProductId, fields);
        return ResponseEntity.ok(result);
    }

    @org.springframework.web.bind.annotation.PutMapping("/products/{localProductId}")
    public ResponseEntity<Map<String, Object>> updatePublishedProduct(
            @PathVariable Long localProductId,
            @RequestBody Map<String, Object> fields) {
        Map<String, Object> result = tiendaNubeService.updatePublishedProduct(localProductId, fields);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/products/{localProductId}/published")
    public ResponseEntity<TiendaNubeProduct> getPublishedProduct(@PathVariable Long localProductId) {
        return ResponseEntity.ok(tiendaNubeService.getPublishedProduct(localProductId));
    }
}
