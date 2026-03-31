package com.mipyme.mercadolibre.service;

import com.mipyme.mercadolibre.dto.MercadoLibreTokenResponse;
import com.mipyme.mercadolibre.dto.MercadoLibreItemDTO;
import com.mipyme.mercadolibre.dto.MercadoLibreItemResponseDTO;
import com.mipyme.mercadolibre.dto.MercadoLibreNotificationDTO;
import com.mipyme.mercadolibre.dto.MercadoLibreOrderSummaryDTO;
import com.mipyme.mercadolibre.dto.MercadoLibreOrderItemDTO;
import com.mipyme.mercadolibre.dto.MercadoLibreBulkSyncRequest;
import com.mipyme.mercadolibre.dto.MercadoLibreSyncPreviewDTO;
import com.mipyme.tiendanube.dto.SyncDifference;
import com.mipyme.mercadolibre.model.messaging.MercadoLibreConversation;
import com.mipyme.mercadolibre.model.messaging.MercadoLibreMessage;
import com.mipyme.mercadolibre.model.messaging.MercadoLibreNotificationLog;
import com.mipyme.mercadolibre.model.MercadoLibreConfig;
import com.mipyme.mercadolibre.repository.MercadoLibreConversationRepository;
import com.mipyme.mercadolibre.repository.MercadoLibreMessageRepository;
import com.mipyme.mercadolibre.repository.MercadoLibreNotificationLogRepository;
import com.mipyme.mercadolibre.repository.MercadoLibreConfigRepository;
import com.mipyme.orders.model.LocalOrderTracking;
import com.mipyme.orders.repository.LocalOrderTrackingRepository;
import com.mipyme.stock.model.Product;
import com.mipyme.stock.model.ProductVariant;
import com.mipyme.stock.repository.ProductRepository;
import com.mipyme.notification.NotificationService;
import com.mipyme.notification.model.Notification.NotificationType;
import com.mipyme.stock.repository.ProductVariantRepository;
import com.mipyme.company.CompanySchemaService;
import com.mipyme.company.Company;
import com.mipyme.company.CompanyRepository;
import com.mipyme.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Service
public class MercadoLibreService {
    private static final Logger logger = LoggerFactory.getLogger(MercadoLibreService.class);

    @Value("${mercadolibre.app-id}")
    private String appId;

    @Value("${mercadolibre.client-secret}")
    private String clientSecret;

    @Value("${mercadolibre.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();
    private final MercadoLibreConfigRepository configRepository;
    private final CompanySchemaService companySchemaService;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final MercadoLibreNotificationLogRepository logRepository;
    private final MercadoLibreConversationRepository conversationRepository;
    private final MercadoLibreMessageRepository messageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SimpMessagingTemplate messagingTemplate;
    private final LocalOrderTrackingRepository localOrderTrackingRepository;
    private final NotificationService notificationService;

    public MercadoLibreService(MercadoLibreConfigRepository configRepository,
            CompanySchemaService companySchemaService,
            CompanyRepository companyRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            MercadoLibreNotificationLogRepository logRepository,
            MercadoLibreConversationRepository conversationRepository,
            MercadoLibreMessageRepository messageRepository,
            SimpMessagingTemplate messagingTemplate,
            LocalOrderTrackingRepository localOrderTrackingRepository,
            NotificationService notificationService) {
        this.configRepository = configRepository;
        this.companySchemaService = companySchemaService;
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.logRepository = logRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.localOrderTrackingRepository = localOrderTrackingRepository;
        this.notificationService = notificationService;
    }

    public MercadoLibreTokenResponse exchangeToken(String code, String codeVerifier) {
        String currentSchema = TenantContext.getCurrentTenant();
        if (currentSchema != null) {
            companySchemaService.createMercadoLibreConfigTable(currentSchema);
        }

        String url = "https://api.mercadolibre.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", appId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("code_verifier", codeVerifier);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<MercadoLibreTokenResponse> response = restTemplate.postForEntity(url, request,
                    MercadoLibreTokenResponse.class);
            MercadoLibreTokenResponse tokenResponse = response.getBody();
            if (tokenResponse != null) {
                saveToken(tokenResponse);
            }
            return tokenResponse;
        } catch (Exception e) {
            logger.error("Error exchanging token with MercadoLibre: {}", e.getMessage());
            throw new RuntimeException("Failed to exchange token", e);
        }
    }

    public MercadoLibreTokenResponse refreshToken() {
        MercadoLibreConfig config = getConfig();
        if (config == null || config.getRefreshToken() == null) {
            throw new RuntimeException("No refresh token available");
        }

        String url = "https://api.mercadolibre.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", appId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", config.getRefreshToken());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<MercadoLibreTokenResponse> response = restTemplate.postForEntity(url, request,
                    MercadoLibreTokenResponse.class);
            MercadoLibreTokenResponse tokenResponse = response.getBody();
            if (tokenResponse != null) {
                saveToken(tokenResponse);
            }
            return tokenResponse;
        } catch (Exception e) {
            logger.error("Error refreshing token: {}", e.getMessage());
            throw new RuntimeException("Failed to refresh token", e);
        }
    }

    @Transactional
    public void syncProducts(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty())
            return;

        MercadoLibreConfig config = getConfig();
        if (config == null || config.getAccessToken() == null) {
            throw new RuntimeException("Not connected");
        }


        if (config.getExpiresAt() != null && config.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
            refreshToken();
            config = getConfig();
        }

        String accessToken = config.getAccessToken();


        int batchSize = 20;
        for (int i = 0; i < itemIds.size(); i += batchSize) {
            int end = Math.min(itemIds.size(), i + batchSize);
            List<String> batch = itemIds.subList(i, end);
            String ids = String.join(",", batch);

            String itemsUrl = "https://api.mercadolibre.com/items?ids=" + ids;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            try {
                ResponseEntity<MercadoLibreItemResponseDTO[]> itemsResponse = restTemplate.exchange(itemsUrl,
                        HttpMethod.GET, entity, MercadoLibreItemResponseDTO[].class);

                if (itemsResponse.getBody() != null) {
                    for (MercadoLibreItemResponseDTO response : itemsResponse.getBody()) {
                        if (response.getCode() == 200 && response.getBody() != null) {
                            syncItem(response.getBody());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error fetching batch items: {}", e.getMessage());
            }
        }
    }

    private void syncItem(MercadoLibreItemDTO item) {
        Optional<Product> existingProduct = productRepository.findByMercadoLibreId(item.getId());
        Product product;

        if (existingProduct.isPresent()) {



            return;
        }

        product = new Product();
        product.setMercadoLibreId(item.getId());
        product.setProductName(item.getTitle());
        if (item.getPrice() != null) {
            product.setPrice(BigDecimal.valueOf(item.getPrice()));
        }
        if (item.getAvailableQuantity() != null) {
            product.setStockQuantity(BigDecimal.valueOf(item.getAvailableQuantity()));
        }
        product.setIsActive(true);


        if (item.getVariations() != null && !item.getVariations().isEmpty()) {
            List<ProductVariant> variants = new ArrayList<>();
            for (var variation : item.getVariations()) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setMercadoLibreId(String.valueOf(variation.getId()));
                if (variation.getPrice() != null) {
                    variant.setPrice(BigDecimal.valueOf(variation.getPrice()));
                }
                if (variation.getAvailableQuantity() != null) {
                    variant.setStockQuantity(BigDecimal.valueOf(variation.getAvailableQuantity()));
                }

                variant.setVariantSku("ML-" + variation.getId());
                variants.add(variant);
            }
            product.setProductVariants(variants);
        }

        productRepository.save(product);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<MercadoLibreItemDTO> getProducts() {
        MercadoLibreConfig config = getConfig();
        if (config == null || config.getAccessToken() == null) {
            throw new RuntimeException("No access token available");
        }


        if (config.getExpiresAt() != null && config.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
            refreshToken();

            config = getConfig();
        }

        String accessToken = config.getAccessToken();
        Long userId = config.getUserId();

        if (userId == null) {
            throw new RuntimeException("User ID not found in configuration");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            Set<String> allItemIds = new LinkedHashSet<>();
            List<String> statuses = Arrays.asList("active", "paused", "closed", "under_review", "inactive", "pending",
                    "deleted");
            for (String st : statuses) {
                try {
                    String searchUrl = "https://api.mercadolibre.com/users/" + userId + "/items/search?status=" + st;
                    ResponseEntity<Map> searchResponse = restTemplate.exchange(searchUrl, HttpMethod.GET, entity,
                            Map.class);
                    Map<String, Object> searchBody = searchResponse.getBody();
                    if (searchBody != null && searchBody.containsKey("results")) {
                        List<String> itemIds = (List<String>) searchBody.get("results");
                        if (itemIds != null && !itemIds.isEmpty()) {
                            allItemIds.addAll(itemIds);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Warning: Could not fetch products for status {}. Reason: {}", st, e.getMessage());
                }
            }

            if (allItemIds.isEmpty()) {
                return new ArrayList<>();
            }

            List<MercadoLibreItemDTO> allItems = new ArrayList<>();
            int batchSize = 20;
            List<String> idsList = new ArrayList<>(allItemIds);
            for (int i = 0; i < idsList.size(); i += batchSize) {
                int end = Math.min(idsList.size(), i + batchSize);
                List<String> batch = idsList.subList(i, end);
                String ids = String.join(",", batch);
                String itemsUrl = "https://api.mercadolibre.com/items?ids=" + ids;
                ResponseEntity<MercadoLibreItemResponseDTO[]> itemsResponse = restTemplate.exchange(itemsUrl,
                        HttpMethod.GET, entity, MercadoLibreItemResponseDTO[].class);
                if (itemsResponse.getBody() != null) {
                    Arrays.stream(itemsResponse.getBody())
                            .filter(r -> r.getCode() == 200 && r.getBody() != null)
                            .map(MercadoLibreItemResponseDTO::getBody)
                            .forEach(allItems::add);
                }
            }
            return allItems;
        } catch (Exception e) {
            logger.error("Error fetching products from MercadoLibre: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch products", e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<MercadoLibreOrderSummaryDTO> getOrders() {
        MercadoLibreConfig config = getConfig();
        if (config == null || config.getAccessToken() == null) {
            logger.info("MercadoLibre no está conectado - no hay token de acceso disponible");
            return new ArrayList<>();
        }
        if (config.getExpiresAt() != null && config.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
            refreshToken();
            config = getConfig();
        }
        String accessToken = config.getAccessToken();
        Long userId = config.getUserId();
        if (userId == null) {
            logger.info("MercadoLibre no está conectado - no hay ID de usuario en la configuración");
            return new ArrayList<>();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<MercadoLibreOrderSummaryDTO> result = new ArrayList<>();
        try {
            String searchUrl = "https://api.mercadolibre.com/orders/search?seller=" + userId
                    + "&sort=date_desc&limit=50";
            ResponseEntity<Map> searchResponse = restTemplate.exchange(searchUrl, HttpMethod.GET, entity, Map.class);
            Map<String, Object> searchBody = searchResponse.getBody();
            if (searchBody == null)
                return result;
            List<Map<String, Object>> orders = (List<Map<String, Object>>) searchBody.get("results");
            if (orders == null)
                return result;

            for (Map<String, Object> ord : orders) {
                Number idNum = (Number) ord.get("id");
                if (idNum == null)
                    continue;
                Long orderId = idNum.longValue();



                Map<String, Object> orderBody = ord;

                MercadoLibreOrderSummaryDTO dto = new MercadoLibreOrderSummaryDTO();
                dto.setOrderId(orderId);

                String mlStatus = (String) orderBody.get("status");

                dto.setOrderStatus(mlStatus);
                dto.setDateCreated((String) orderBody.get("date_created"));
                Map<String, Object> buyer = (Map<String, Object>) orderBody.get("buyer");
                if (buyer != null) {
                    dto.setBuyerNickname((String) buyer.get("nickname"));
                    dto.setBuyerEmail((String) buyer.get("email"));
                    Number buyerIdNum = (Number) buyer.get("id");
                    if (buyerIdNum != null) {
                        dto.setBuyerId(buyerIdNum.longValue());
                    }
                }
                Object totalAmountObj = orderBody.get("total_amount");
                if (totalAmountObj instanceof Number) {
                    dto.setTotalAmount(BigDecimal.valueOf(((Number) totalAmountObj).doubleValue()));
                }
                dto.setCurrencyId((String) orderBody.get("currency_id"));


                List<Map<String, Object>> orderItems = (List<Map<String, Object>>) orderBody.get("order_items");
                if (orderItems != null) {
                    List<MercadoLibreOrderItemDTO> itemsList = new ArrayList<>();
                    for (Map<String, Object> oi : orderItems) {
                        MercadoLibreOrderItemDTO itemDto = new MercadoLibreOrderItemDTO();
                        Map<String, Object> itemData = (Map<String, Object>) oi.get("item");
                        if (itemData != null) {
                            itemDto.setId((String) itemData.get("id"));
                            itemDto.setTitle((String) itemData.get("title"));
                        }


                        Object qtyObj = oi.get("quantity");
                        if (qtyObj instanceof Number) {
                            itemDto.setQuantity(((Number) qtyObj).intValue());
                        } else {
                            itemDto.setQuantity(1);
                        }


                        Object priceObj = oi.get("unit_price");
                        if (priceObj instanceof Number) {
                            itemDto.setUnitPrice(BigDecimal.valueOf(((Number) priceObj).doubleValue()));
                        } else {
                            itemDto.setUnitPrice(BigDecimal.ZERO);
                        }

                        itemDto.setCurrencyId((String) oi.get("currency_id"));
                        itemsList.add(itemDto);
                    }
                    dto.setItems(itemsList);
                }

                Number packIdNum = (Number) orderBody.get("pack_id");
                if (packIdNum != null) {
                    dto.setPackId(packIdNum.longValue());
                }

                Map<String, Object> shipping = (Map<String, Object>) orderBody.get("shipping");
                if (shipping != null) {
                    Number shipIdNum = (Number) shipping.get("id");
                    if (shipIdNum != null) {
                        dto.setShippingId(shipIdNum.longValue());
                        if (shipping.containsKey("status")) {
                            dto.setShippingStatus((String) shipping.get("status"));
                        }
                    }
                }

                result.add(dto);
            }


            List<Long> missingShipmentIds = result.stream()
                    .filter(o -> o.getShippingId() != null && o.getShippingStatus() == null)
                    .map(MercadoLibreOrderSummaryDTO::getShippingId)
                    .distinct()
                    .collect(Collectors.toList());

            if (!missingShipmentIds.isEmpty()) {

                int shipBatchSize = 50;
                for (int i = 0; i < missingShipmentIds.size(); i += shipBatchSize) {
                    int end = Math.min(missingShipmentIds.size(), i + shipBatchSize);
                    List<Long> batch = missingShipmentIds.subList(i, end);
                    String ids = batch.stream().map(String::valueOf).collect(Collectors.joining(","));

                    try {
                        String shipUrl = "https://api.mercadolibre.com/shipments?ids=" + ids;
                        ResponseEntity<Object[]> shipResp = restTemplate.exchange(shipUrl, HttpMethod.GET, entity,
                                Object[].class);
                        Object[] responses = shipResp.getBody();

                        if (responses != null) {
                            for (Object respObj : responses) {
                                if (respObj instanceof Map) {
                                    Map<String, Object> respMap = (Map<String, Object>) respObj;
                                    Integer code = (Integer) respMap.get("code");

                                    if (code != null && code == 200) {
                                        Map<String, Object> body = (Map<String, Object>) respMap.get("body");
                                        if (body != null) {
                                            Number idNum = (Number) body.get("id");
                                            String status = (String) body.get("status");

                                            if (idNum != null && status != null) {
                                                Long shipId = idNum.longValue();
                                                result.stream()
                                                        .filter(o -> o.getShippingId() != null
                                                                && o.getShippingId().equals(shipId))
                                                        .forEach(o -> {
                                                            o.setShippingStatus(status);

                                                            Object sub = body.get("substatus");
                                                            if (sub != null)
                                                                o.setShippingSubstatus(sub.toString());
                                                        });
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Error batch fetching shipments: {}", e.getMessage());
                    }
                }
            }


            for (MercadoLibreOrderSummaryDTO order : result) {
                try {
                    String localStatusVal = "NOT_FOUND";
                    java.util.Optional<LocalOrderTracking> localTracking = localOrderTrackingRepository
                            .findByExternalIdAndSource(String.valueOf(order.getOrderId()), "mercadolibre");

                    if (localTracking.isPresent()) {
                        String rawLocalStatus = localTracking.get().getLocalStatus();
                        localStatusVal = rawLocalStatus;

                        if (rawLocalStatus != null) {
                            String normalizedLocal = rawLocalStatus.toLowerCase();



                            if (normalizedLocal.equals("delivered") ||
                                    normalizedLocal.equals("shipped") ||
                                    normalizedLocal.equals("ready_to_ship") ||
                                    normalizedLocal.equals("pending") ||
                                    normalizedLocal.equals("handling") ||
                                    normalizedLocal.equals("not_delivered")) {

                                logger.info(
                                        "[MERCADOLIBRE] Applying local override for Order {}: Shipping Status {} -> {}",
                                        order.getOrderId(), order.getShippingStatus(), normalizedLocal);
                                order.setShippingStatus(normalizedLocal);
                            }

                            else if (normalizedLocal.equals("cancelled") ||
                                    normalizedLocal.equals("paid") ||
                                    normalizedLocal.equals("closed")) {

                                logger.info(
                                        "[MERCADOLIBRE] Applying local override for Order {}: Order Status {} -> {}",
                                        order.getOrderId(), order.getOrderStatus(), normalizedLocal);
                                order.setOrderStatus(normalizedLocal);
                            }

                            else {
                                logger.info(
                                        "[MERCADOLIBRE] Local status '{}' found but not explicitly mapped. Setting as Shipping Status.",
                                        normalizedLocal);
                                order.setShippingStatus(normalizedLocal);
                            }
                        }
                    }

                    logger.info(
                            "[MERCADOLIBRE] Order Debug - ID: {}, API Status: {}, API ShipStatus: {}, Local Status: {}",
                            order.getOrderId(), order.getOrderStatus(), order.getShippingStatus(), localStatusVal);

                } catch (Exception ex) {
                    logger.error("[MERCADOLIBRE] Error logging/overriding order info: {}", ex.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Error fetching orders: {}", e.getMessage());
        }


        for (MercadoLibreOrderSummaryDTO order : result) {
            try {
                String refKey = "ml-order-" + order.getOrderId();
                String buyer = order.getBuyerNickname() != null ? order.getBuyerNickname() : "comprador";
                String msg = "Orden MercadoLibre #" + order.getOrderId()
                        + " de " + buyer
                        + " - $" + (order.getTotalAmount() != null ? order.getTotalAmount() : "0");
                notificationService.createIfNotExists(
                        NotificationType.ML_WEBHOOK, msg, "/mercadolibre", refKey);
            } catch (Exception ex) {

            }
        }

        return result;
    }

    private void saveToken(MercadoLibreTokenResponse tokenResponse) {
        MercadoLibreConfig config = configRepository.findTopByOrderByIdDesc().orElse(new MercadoLibreConfig());
        config.setAccessToken(tokenResponse.getAccessToken());
        config.setRefreshToken(tokenResponse.getRefreshToken());
        config.setTokenType(tokenResponse.getTokenType());
        config.setScope(tokenResponse.getScope());
        config.setUserId(tokenResponse.getUserId());


        if (tokenResponse.getExpiresIn() != null) {
            config.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));
        }

        configRepository.save(config);
    }

    public void handleNotification(MercadoLibreNotificationDTO notification) {
        logger.info("Received Webhook Notification: {}", notification);
        Long mlUserId = notification.getUserId();
        String tenantSchema = resolveTenantSchemaByMeliUser(mlUserId);
        if (tenantSchema == null) {
            logger.warn("No tenant schema found for MercadoLibre user_id={}", mlUserId);
            return;
        }
        logger.info("Resolved tenant schema {} for MercadoLibre user_id={}", tenantSchema, mlUserId);
        TenantContext.setCurrentTenant(tenantSchema);
        try {
            MercadoLibreNotificationLog log = new MercadoLibreNotificationLog();
            try {
                log.setPayload(objectMapper.writeValueAsString(notification));
            } catch (Exception e) {
                log.setPayload(notification.toString());
            }
            log.setTopic(notification.getTopic());
            log.setResource(notification.getResource());
            log.setUserId(notification.getUserId());
            log.setStatus(MercadoLibreNotificationLog.NotificationStatus.PENDING);
            log.setReceivedAt(LocalDateTime.now());

            MercadoLibreNotificationLog savedLog = logRepository.save(log);
            processNotificationAsyncTenant(savedLog.getId(), tenantSchema);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveTenantSchemaByMeliUser(Long mlUserId) {
        if (mlUserId == null)
            return null;
        List<Company> companies = companyRepository.findAll();
        for (Company company : companies) {
            String schema = company.getTenantSchema();
            if (schema == null || schema.isBlank())
                continue;
            TenantContext.setCurrentTenant(schema);
            try {
                MercadoLibreConfig cfg = configRepository.findTopByOrderByIdDesc().orElse(null);
                if (cfg != null && mlUserId.equals(cfg.getUserId())) {
                    return schema;
                }
            } finally {
                TenantContext.clear();
            }
        }
        return null;
    }

    @Async
    public void processNotificationAsyncTenant(Long logId, String tenantSchema) {
        TenantContext.setCurrentTenant(tenantSchema);
        try {
            processNotificationAsync(logId);
        } finally {
            TenantContext.clear();
        }
    }

    @Async
    public void processNotificationAsync(Long logId) {
        MercadoLibreNotificationLog log = logRepository.findById(logId).orElse(null);
        if (log == null)
            return;

        try {
            if (log.getTopic() == null) {
                log.setStatus(MercadoLibreNotificationLog.NotificationStatus.PROCESSED);
                return;
            }

            switch (log.getTopic()) {
                case "items":
                    if (log.getResource() != null) {
                        String itemId = log.getResource().substring(log.getResource().lastIndexOf("/") + 1);
                        logger.info("Item update notification for: {}", itemId);
                        notificationService.createIfNotExists(
                                NotificationType.ML_WEBHOOK,
                                "Actualización de publicación en MercadoLibre: " + itemId,
                                "/mercadolibre",
                                "ml-item-" + itemId);
                    }
                    break;
                case "messages":
                    if (log.getResource() != null) {
                        String res = log.getResource();
                        logger.info("Message notification for: {}", res);
                        String packId = null;
                        int idxPacks = res.indexOf("/packs/");
                        if (idxPacks >= 0) {
                            String tail = res.substring(idxPacks + "/packs/".length());
                            packId = tail.contains("/") ? tail.substring(0, tail.indexOf('/')) : tail;
                        }
                        MercadoLibreConfig cfg = getConfig();
                        Long myUserId = cfg != null ? cfg.getUserId() : log.getUserId();
                        if (packId != null && !packId.isBlank() && myUserId != null) {
                            syncConversationMessages(packId, myUserId);
                        } else if (myUserId != null) {
                            syncUnreadPacks(myUserId);
                        }
                        notificationService.createIfNotExists(
                                NotificationType.MESSAGE,
                                "Nuevo mensaje en MercadoLibre",
                                "/mensajeria",
                                "ml-msg-" + log.getId());
                    }
                    break;
                case "questions":
                    logger.info("Questions notification: {}", log.getResource());
                    notificationService.createIfNotExists(
                            NotificationType.ML_WEBHOOK,
                            "Nueva pregunta en MercadoLibre",
                            "/mercadolibre",
                            "ml-question-" + log.getId());
                    break;
                case "orders_v2":
                case "orders":
                case "created_orders":
                    logger.info("Order notification: {}", log.getResource());
                    notificationService.createIfNotExists(
                            NotificationType.ML_WEBHOOK,
                            "Nueva orden en MercadoLibre",
                            "/mercadolibre",
                            "ml-order-" + log.getId());
                    break;
                default:
                    logger.info("Unhandled topic: {}", log.getTopic());
            }

            log.setStatus(MercadoLibreNotificationLog.NotificationStatus.PROCESSED);
            log.setProcessedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.setStatus(MercadoLibreNotificationLog.NotificationStatus.ERROR);
            log.setErrorMessage(e.getMessage());
            logger.error("Error processing notification {}: {}", logId, e.getMessage());
        } finally {
            logRepository.save(log);
        }
    }

    public void updateLocalStatus(String orderId, String status) {
        LocalOrderTracking tracking = localOrderTrackingRepository.findByExternalIdAndSource(orderId, "mercadolibre")
                .orElse(new LocalOrderTracking());

        if (tracking.getId() == null) {
            tracking.setExternalId(orderId);
            tracking.setSource("mercadolibre");
        }
        tracking.setLocalStatus(status);
        tracking.setUpdatedAt(LocalDateTime.now());
        localOrderTrackingRepository.save(tracking);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void syncUnreadPacks(Long myUserId) {
        MercadoLibreConfig config = getConfig();
        if (config == null || config.getAccessToken() == null)
            return;
        String url = "https://api.mercadolibre.com/messages/unread?role=seller&tag=post_sale&user_id=" + myUserId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(config.getAccessToken());
        headers.add("X-Caller-Id", String.valueOf(myUserId));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            if (body == null)
                return;
            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
            if (results == null)
                return;
            for (Map<String, Object> r : results) {
                Object resObj = r.get("resource");
                if (!(resObj instanceof String))
                    continue;
                String res = (String) resObj;
                String packId = null;
                int idxPacks = res.indexOf("/packs/");
                if (idxPacks >= 0) {
                    String tail = res.substring(idxPacks + "/packs/".length());
                    packId = tail.contains("/") ? tail.substring(0, tail.indexOf('/')) : tail;
                }
                if (packId != null && !packId.isBlank()) {
                    logger.info("Syncing unread pack_id={}", packId);
                    syncConversationMessages(packId, myUserId);
                }
            }
        } catch (Exception e) {
            logger.error("Error syncing unread packs: {}", e.getMessage());
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void syncConversationMessages(String conversationId, Long myUserId) {
        MercadoLibreConfig config = getConfig();
        if (config == null || config.getAccessToken() == null)
            return;

        String url = "https://api.mercadolibre.com/messages/packs/" + conversationId + "/sellers/" + myUserId
                + "?mark_as_read=false&limit=50&tag=post_sale";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(config.getAccessToken());
        headers.set("Accept", "application/json");
        headers.add("X-Caller-Id", String.valueOf(myUserId));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = (Map<String, Object>) response.getBody();

            if (body == null)
                return;


            List<Map<String, Object>> messagesData = (List<Map<String, Object>>) body.get("messages");
            if (messagesData == null)
                return;


            Long packId = Long.parseLong(conversationId);
            MercadoLibreConversation conversation = conversationRepository.findById(packId).orElse(null);
            if (conversation == null) {
                conversation = new MercadoLibreConversation();
                conversation.setId(packId);
                conversation.setSellerId(myUserId);
                conversationRepository.save(conversation);
            }








            List<MercadoLibreMessage> newMessages = new ArrayList<>();
            List<Map<String, Object>> broadcastMessages = new ArrayList<>();

            for (Map<String, Object> msgData : messagesData) {
                String msgId = (String) msgData.get("id");

                if (messageRepository.existsById(msgId))
                    continue;

                MercadoLibreMessage msg = new MercadoLibreMessage();
                msg.setId(msgId);
                msg.setConversation(conversation);

                Map<String, Object> from = (Map<String, Object>) msgData.get("from");
                Map<String, Object> to = (Map<String, Object>) msgData.get("to");

                msg.setFromUserId(((Number) from.get("user_id")).longValue());
                msg.setToUserId(((Number) to.get("user_id")).longValue());


                if (!msg.getFromUserId().equals(myUserId)) {
                    conversation.setBuyerId(msg.getFromUserId());
                } else if (!msg.getToUserId().equals(myUserId)) {
                    conversation.setBuyerId(msg.getToUserId());
                }








                Object textObj = msgData.get("text");
                if (textObj instanceof String) {
                    msg.setText((String) textObj);
                } else if (textObj instanceof Map) {
                    Map<String, Object> textMap = (Map<String, Object>) textObj;
                    msg.setText((String) textMap.get("plain"));
                }

                msg.setStatus((String) msgData.get("status"));
                Map<String, Object> md = (Map<String, Object>) msgData.get("message_date");
                String created = md != null ? (String) md.get("created") : null;
                String available = md != null ? (String) md.get("available") : null;
                String received = md != null ? (String) md.get("received") : null;
                String dateStr = created != null ? created : (available != null ? available : received);
                if (dateStr != null) {
                    msg.setDateCreated(LocalDateTime.parse(dateStr.replace("Z", "")));
                }



                newMessages.add(msg);
                messageRepository.save(msg);
                Map<String, Object> payload = new HashMap<>();
                payload.put("id", msg.getId());
                payload.put("conversationId", conversation.getId());
                payload.put("fromUserId", msg.getFromUserId());
                payload.put("toUserId", msg.getToUserId());
                payload.put("text", msg.getText());
                payload.put("status", msg.getStatus());
                payload.put("dateCreated", msg.getDateCreated() != null ? msg.getDateCreated().toString() : null);
                broadcastMessages.add(payload);
            }


            if (!messagesData.isEmpty()) {
                Map<String, Object> lastMsg = messagesData.get(messagesData.size() - 1);
                Map<String, Object> md = (Map<String, Object>) lastMsg.get("message_date");
                String created = md != null ? (String) md.get("created") : null;
                String available = md != null ? (String) md.get("available") : null;
                String received = md != null ? (String) md.get("received") : null;
                String dateStr = created != null ? created : (available != null ? available : received);
                if (dateStr != null) {
                    conversation.setLastMessageDate(LocalDateTime.parse(dateStr.replace("Z", "")));
                }
            }

            conversationRepository.save(conversation);
            if (!broadcastMessages.isEmpty()) {
                messagingTemplate.convertAndSend("/topic/meli/messages/" + conversationId, broadcastMessages);
            }

        } catch (Exception e) {
            logger.error("Error syncing conversation {}: {}", conversationId, e.getMessage());
        }
    }

    public MercadoLibreConfig getConfig() {
        return configRepository.findTopByOrderByIdDesc().orElse(null);
    }

    public String getAppId() {
        return appId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public List<MercadoLibreConversation> getConversations() {
        return conversationRepository.findAll();
    }

    public List<MercadoLibreMessage> getMessages(Long conversationId) {
        return messageRepository.findByConversation_IdOrderByDateCreatedAsc(conversationId);
    }

    public List<MercadoLibreMessage> getMessages(Long conversationId, Integer limit, String before) {
        if (limit == null || limit <= 0) {
            return getMessages(conversationId);
        }
        LocalDateTime beforeTs = null;
        if (before != null && !before.isBlank()) {
            try {
                beforeTs = LocalDateTime.parse(before.replace("Z", ""));
            } catch (Exception ignored) {
            }
        }
        if (beforeTs == null) {
            beforeTs = LocalDateTime.now();
        }
        var pageable = org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit),
                org.springframework.data.domain.Sort.by("dateCreated").descending());
        List<MercadoLibreMessage> desc = messageRepository.findByConversation_IdAndDateCreatedLessThan(conversationId,
                beforeTs, pageable);
        java.util.Collections.reverse(desc);
        return desc;
    }

    public MercadoLibreConversation ensureConversation(Long packId) {
        MercadoLibreConfig config = getConfig();
        if (config == null || config.getAccessToken() == null)
            throw new RuntimeException("No configuration found");
        if (config.getExpiresAt() != null && config.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
            refreshToken();
            config = getConfig();
        }
        syncConversationMessages(String.valueOf(packId), config.getUserId());
        return conversationRepository.findById(packId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
    }

    public MercadoLibreMessage sendMessage(Long conversationId, String text) {
        MercadoLibreConfig config = getConfig();
        if (config == null || config.getAccessToken() == null)
            throw new RuntimeException("No configuration found");

        MercadoLibreConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));


        Long recipientId = conversation.getBuyerId();
        if (recipientId == null)
            throw new RuntimeException("Recipient not identified");

        String url = "https://api.mercadolibre.com/messages/packs/" + conversationId + "/sellers/" + config.getUserId()
                + "?tag=post_sale";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(config.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "from", Map.of("user_id", config.getUserId()),
                "to", Map.of("user_id", recipientId),
                "text", text);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, entity, String.class);



            syncConversationMessages(conversationId.toString(), config.getUserId());

            List<MercadoLibreMessage> messages = messageRepository
                    .findByConversation_IdOrderByDateCreatedAsc(conversationId);
            messagingTemplate.convertAndSend("/topic/meli/messages/" + conversationId, (Object) Map.of("type", "sent"));
            return messages.isEmpty() ? null : messages.get(messages.size() - 1);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send message: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void unlinkAccount() {
        productRepository.unlinkAllMercadoLibreProducts();
        productVariantRepository.unlinkAllMercadoLibreVariants();
        configRepository.deleteAll();
    }

    @Transactional
    public void unlinkProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty())
            return;

        for (Long id : productIds) {
            Product p = productRepository.findById(id).orElse(null);
            if (p != null) {
                p.setMercadoLibreId(null);
                if (p.getProductVariants() != null) {
                    for (ProductVariant v : p.getProductVariants()) {
                        v.setMercadoLibreId(null);
                    }
                }
                productRepository.save(p);
            }
        }
    }

    public List<Product> getLinkedLocalProducts() {
        return productRepository.findByMercadoLibreIdIsNotNull();
    }

    @Transactional
    public List<MercadoLibreSyncPreviewDTO> previewBulkSync(List<MercadoLibreBulkSyncRequest> requests) {
        List<MercadoLibreSyncPreviewDTO> previews = new ArrayList<>();
        MercadoLibreConfig config = getConfig();
        String accessToken = config != null ? config.getAccessToken() : null;

        java.util.Set<String> itemIds = requests.stream()
                .map(MercadoLibreBulkSyncRequest::getMlItemId)
                .collect(java.util.stream.Collectors.toSet());

        Map<String, MercadoLibreItemDTO> mlItemsMap = new HashMap<>();

        List<String> idsList = new ArrayList<>(itemIds);
        int batchSize = 20;
        HttpHeaders headers = new HttpHeaders();
        if (accessToken != null)
            headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        for (int i = 0; i < idsList.size(); i += batchSize) {
            int end = Math.min(idsList.size(), i + batchSize);
            List<String> batch = idsList.subList(i, end);
            String ids = String.join(",", batch);
            String itemsUrl = "https://api.mercadolibre.com/items?ids=" + ids;
            try {
                ResponseEntity<MercadoLibreItemResponseDTO[]> itemsResponse = restTemplate.exchange(itemsUrl,
                        HttpMethod.GET, entity, MercadoLibreItemResponseDTO[].class);
                if (itemsResponse.getBody() != null) {
                    for (MercadoLibreItemResponseDTO r : itemsResponse.getBody()) {
                        if (r.getCode() == 200 && r.getBody() != null) {
                            mlItemsMap.put(r.getBody().getId(), r.getBody());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error fetching items batch " + ids, e);
            }
        }

        for (MercadoLibreBulkSyncRequest req : requests) {
            MercadoLibreItemDTO mlItem = mlItemsMap.get(req.getMlItemId());
            if (mlItem == null)
                continue;

            Product localProduct = null;
            if (Boolean.TRUE.equals(req.getCreateNew())) {

            } else if (req.getLocalProductId() != null) {
                localProduct = productRepository.findByIdWithVariants(req.getLocalProductId()).orElse(null);
            }

            MercadoLibreSyncPreviewDTO preview = new MercadoLibreSyncPreviewDTO(mlItem, localProduct);

            if (localProduct != null) {
                calculateDifferences(preview, mlItem, localProduct);
            }
            previews.add(preview);
        }
        return previews;
    }

    private void calculateDifferences(MercadoLibreSyncPreviewDTO preview, MercadoLibreItemDTO mlItem, Product local) {
        if (!mlItem.getTitle().equals(local.getProductName())) {
            preview.addDifference(
                    new SyncDifference("name", "Product", null, local.getProductName(), mlItem.getTitle()));
        }
        BigDecimal mlPrice = BigDecimal.valueOf(mlItem.getPrice());
        if (local.getPrice() != null && mlPrice.compareTo(local.getPrice()) != 0) {
            preview.addDifference(
                    new SyncDifference("price", "Product", null, local.getPrice().toString(), mlPrice.toString()));
        }
        if (mlItem.getAvailableQuantity() != null) {
            BigDecimal mlStock = BigDecimal.valueOf(mlItem.getAvailableQuantity());
            if (local.getStockQuantity() != null && mlStock.compareTo(local.getStockQuantity()) != 0) {
                preview.addDifference(new SyncDifference("stock", "Product", null, local.getStockQuantity().toString(),
                        mlStock.toString()));
            }
        }
    }

    @Transactional
    public void executeSync(List<MercadoLibreSyncPreviewDTO> previews) {
        MercadoLibreConfig config = getConfig();
        String accessToken = config != null ? config.getAccessToken() : null;

        for (MercadoLibreSyncPreviewDTO preview : previews) {
            MercadoLibreItemDTO mlItem = preview.getMercadoLibreItem();

            if (preview.isNew()) {
                Optional<Product> existing = productRepository.findByMercadoLibreId(mlItem.getId());
                if (existing.isEmpty()) {
                    syncItem(mlItem);
                }
            } else {
                Product local = productRepository.findById(preview.getLocalProduct().getProductId()).orElseThrow();
                local.setMercadoLibreId(mlItem.getId());

                Map<String, Object> remoteUpdates = new HashMap<>();

                for (SyncDifference diff : preview.getDifferences()) {
                    if ("LOCAL".equals(diff.getResolution())) {
                        if ("name".equals(diff.getField()))
                            remoteUpdates.put("title", diff.getLocalValue());
                        if ("price".equals(diff.getField()))
                            remoteUpdates.put("price", new BigDecimal(diff.getLocalValue()));
                        if ("stock".equals(diff.getField()))
                            remoteUpdates.put("available_quantity", new BigDecimal(diff.getLocalValue()));
                    } else if ("REMOTE".equals(diff.getResolution())) {
                        if ("name".equals(diff.getField()))
                            local.setProductName(diff.getRemoteValue());
                        if ("price".equals(diff.getField()))
                            local.setPrice(new BigDecimal(diff.getRemoteValue()));
                        if ("stock".equals(diff.getField()))
                            local.setStockQuantity(new BigDecimal(diff.getRemoteValue()));
                    }
                }
                productRepository.save(local);

                if (!remoteUpdates.isEmpty() && accessToken != null) {
                    updateRemoteItem(mlItem.getId(), remoteUpdates, accessToken);
                }
            }
        }
    }

    private void updateRemoteItem(String itemId, Map<String, Object> updates, String accessToken) {
        String url = "https://api.mercadolibre.com/items/" + itemId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updates, headers);
        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
        } catch (Exception e) {
            logger.error("Error updating remote item " + itemId, e);
        }
    }



    private String getAccessTokenRefreshed() {
        MercadoLibreConfig config = getConfig();
        if (config == null || config.getAccessToken() == null) {
            throw new RuntimeException("No MercadoLibre access token available");
        }
        if (config.getExpiresAt() != null && config.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
            refreshToken();
            config = getConfig();
        }
        return config.getAccessToken();
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Map<String, Object>> listSellerPromotions() {
        String accessToken = getAccessTokenRefreshed();
        MercadoLibreConfig config = getConfig();
        String url = "https://api.mercadolibre.com/seller-promotions/search?seller_id=" + config.getUserId();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            logger.error("Error listing seller promotions: {}", e.getMessage());
            return new ArrayList<>();
        }
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map<String, Object> getItemSalePrice(String itemId) {
        String accessToken = getAccessTokenRefreshed();
        String url = "https://api.mercadolibre.com/items/" + itemId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("price", body.get("price"));
                result.put("sale_price", body.get("sale_price"));
                result.put("original_price", body.get("original_price"));
                return result;
            }
        } catch (Exception e) {
            logger.error("Error getting item sale price for {}: {}", itemId, e.getMessage());
        }
        return new HashMap<>();
    }


    public void applySalePrice(String itemId, BigDecimal salePrice) {
        String accessToken = getAccessTokenRefreshed();
        String url = "https://api.mercadolibre.com/items/" + itemId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("price", salePrice);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
    }


    public void removeSalePrice(String itemId, BigDecimal originalPrice) {
        String accessToken = getAccessTokenRefreshed();
        String url = "https://api.mercadolibre.com/items/" + itemId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("price", originalPrice);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
    }



    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Transactional
    public Map<String, Object> publishProduct(Long localProductId, Map<String, Object> mlFields) {
        String accessToken = getAccessTokenRefreshed();
        MercadoLibreConfig config = getConfig();
        if (config == null || config.getUserId() == null) {
            throw new RuntimeException("MercadoLibre not connected");
        }

        Product localProduct = productRepository.findByIdWithVariants(localProductId)
                .orElseThrow(() -> new RuntimeException("Local product not found: " + localProductId));

        if (localProduct.getMercadoLibreId() != null) {
            throw new RuntimeException(
                    "Product already published to MercadoLibre (ID: " + localProduct.getMercadoLibreId() + ")");
        }


        Map<String, Object> body = new HashMap<>();
        body.put("title", mlFields.getOrDefault("title", localProduct.getProductName()));
        body.put("category_id", mlFields.get("category_id"));
        body.put("condition", mlFields.getOrDefault("condition", "new"));
        body.put("listing_type_id", mlFields.getOrDefault("listing_type_id", "gold_special"));
        body.put("currency_id", mlFields.getOrDefault("currency_id", "ARS"));


        if (localProduct.getPrice() != null) {
            body.put("price", localProduct.getPrice());
        }
        if (mlFields.containsKey("price")) {
            body.put("price", mlFields.get("price"));
        }


        if (localProduct.getStockQuantity() != null) {
            body.put("available_quantity", localProduct.getStockQuantity().intValue());
        }
        if (mlFields.containsKey("available_quantity")) {
            body.put("available_quantity", mlFields.get("available_quantity"));
        }



        if (mlFields.containsKey("description")) {
            Map<String, String> desc = new HashMap<>();
            desc.put("plain_text", String.valueOf(mlFields.get("description")));
            body.put("description", desc);
        }


        if (mlFields.containsKey("pictures")) {
            List<Map<String, String>> pictures = new ArrayList<>();
            Object pics = mlFields.get("pictures");
            if (pics instanceof List) {
                for (Object p : (List<?>) pics) {
                    if (p instanceof String) {
                        Map<String, String> pic = new HashMap<>();
                        pic.put("source", (String) p);
                        pictures.add(pic);
                    } else if (p instanceof Map) {
                        pictures.add((Map<String, String>) p);
                    }
                }
            }
            body.put("pictures", pictures);
        }


        if (mlFields.containsKey("attributes")) {
            body.put("attributes", mlFields.get("attributes"));
        }

        String url = "https://api.mercadolibre.com/items";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            logger.info("Publishing to ML body: {}", objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            logger.error("Error serializing body for logging", e);
        }

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, req, Map.class);
            Map<String, Object> created = response.getBody();
            if (created != null) {
                String mlId = (String) created.get("id");
                if (mlId != null) {
                    localProduct.setMercadoLibreId(mlId);
                    productRepository.save(localProduct);
                }
            }
            logger.info("Published local product {} to MercadoLibre", localProductId);
            return created;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String mlError = extractMlErrorMessage(e);
            logger.error("Error publishing to MercadoLibre: {}", mlError);
            throw new RuntimeException(mlError, e);
        } catch (Exception e) {
            logger.error("Error publishing to MercadoLibre: {}", e.getMessage());
            throw new RuntimeException("Error inesperado al publicar: " + e.getMessage(), e);
        } finally {

            updateLocalProductFromPublication(localProduct, mlFields);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Transactional
    public Map<String, Object> updatePublishedProduct(Long localProductId, Map<String, Object> mlFields) {
        String accessToken = getAccessTokenRefreshed();

        Product localProduct = productRepository.findByIdWithVariants(localProductId)
                .orElseThrow(() -> new RuntimeException("Local product not found: " + localProductId));

        if (localProduct.getMercadoLibreId() == null) {
            throw new RuntimeException("Product is not published to MercadoLibre");
        }

        Map<String, Object> body = new HashMap<>();
        if (mlFields.containsKey("title"))
            body.put("title", mlFields.get("title"));
        if (mlFields.containsKey("price"))
            body.put("price", mlFields.get("price"));
        if (mlFields.containsKey("available_quantity"))
            body.put("available_quantity", mlFields.get("available_quantity"));
        if (mlFields.containsKey("pictures")) {
            List<Map<String, String>> pictures = new ArrayList<>();
            Object pics = mlFields.get("pictures");
            if (pics instanceof List) {
                for (Object p : (List<?>) pics) {
                    if (p instanceof String) {
                        Map<String, String> pic = new HashMap<>();
                        pic.put("source", (String) p);
                        pictures.add(pic);
                    } else if (p instanceof Map) {
                        pictures.add((Map<String, String>) p);
                    }
                }
            }
            body.put("pictures", pictures);
        }

        String url = "https://api.mercadolibre.com/items/" + localProduct.getMercadoLibreId();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            logger.info("Updating ML item body: {}", objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            logger.error("Error serializing body for logging", e);
        }

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, req, Map.class);
            logger.info("Updated MercadoLibre item {}", localProduct.getMercadoLibreId());
            return response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String mlError = extractMlErrorMessage(e);
            logger.error("Error updating MercadoLibre item: {}", mlError);
            throw new RuntimeException(mlError, e);
        } catch (Exception e) {
            logger.error("Error updating MercadoLibre item: {}", e.getMessage());
            throw new RuntimeException("Error inesperado al actualizar: " + e.getMessage(), e);
        } finally {

            updateLocalProductFromPublication(localProduct, mlFields);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map<String, Object> updateItemDirect(String mlItemId, Map<String, Object> mlFields) {
        String accessToken = getAccessTokenRefreshed();

        Map<String, Object> body = new HashMap<>();
        if (mlFields.containsKey("title"))
            body.put("title", mlFields.get("title"));
        if (mlFields.containsKey("price"))
            body.put("price", mlFields.get("price"));
        if (mlFields.containsKey("available_quantity"))
            body.put("available_quantity", mlFields.get("available_quantity"));
        if (mlFields.containsKey("status"))
            body.put("status", mlFields.get("status"));
        if (mlFields.containsKey("pictures")) {
            List<Map<String, String>> pictures = new ArrayList<>();
            Object pics = mlFields.get("pictures");
            if (pics instanceof List) {
                for (Object p : (List<?>) pics) {
                    if (p instanceof String) {
                        Map<String, String> pic = new HashMap<>();
                        pic.put("source", (String) p);
                        pictures.add(pic);
                    } else if (p instanceof Map) {
                        pictures.add((Map<String, String>) p);
                    }
                }
            }
            body.put("pictures", pictures);
        }

        String url = "https://api.mercadolibre.com/items/" + mlItemId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            logger.info("Updating ML item {} direct body: {}", mlItemId, objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            logger.error("Error serializing body for logging", e);
        }

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, req, Map.class);
            logger.info("Updated MercadoLibre direct item {}", mlItemId);
            return response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String mlError = extractMlErrorMessage(e);
            logger.error("Error updating MercadoLibre item {}: {}", mlItemId, mlError);
            throw new RuntimeException(mlError, e);
        } catch (Exception e) {
            logger.error("Error updating MercadoLibre item {}: {}", mlItemId, e.getMessage());
            throw new RuntimeException("Error inesperado al actualizar: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Transactional
    public Map<String, Object> deleteItemDirect(String mlItemId) {
        String accessToken = getAccessTokenRefreshed();



        Map<String, Object> body = new HashMap<>();
        body.put("status", "closed");
        body.put("deleted", true);

        String url = "https://api.mercadolibre.com/items/" + mlItemId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        Map<String, Object> result = null;
        boolean alreadyTerminal = false;
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, req, Map.class);
            logger.info("Deleted/closed MercadoLibre item {}", mlItemId);
            result = response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String mlError = extractMlErrorMessage(e);


            if (mlError != null && mlError.toLowerCase().contains("not modifiable")) {
                logger.info("ML item {} is already in a terminal state, proceeding to unlink locally", mlItemId);
                alreadyTerminal = true;
            } else {
                logger.error("Error deleting MercadoLibre item {}: {}", mlItemId, mlError);
                throw new RuntimeException(mlError, e);
            }
        } catch (Exception e) {
            logger.error("Error deleting MercadoLibre item {}: {}", mlItemId, e.getMessage());
            throw new RuntimeException("Error inesperado al eliminar: " + e.getMessage(), e);
        }


        List<Product> linkedProducts = productRepository.findAll().stream()
                .filter(p -> mlItemId.equals(p.getMercadoLibreId()))
                .collect(java.util.stream.Collectors.toList());
        for (Product lp : linkedProducts) {
            logger.info("Unlinking local product {} from deleted ML item {}", lp.getId(), mlItemId);
            lp.setMercadoLibreId(null);
            productRepository.save(lp);
        }

        if (alreadyTerminal) {
            return Map.of("status", "already_deleted", "message",
                    "Publicación ya estaba cerrada/eliminada. Se desvinculó el producto local.");
        }
        return result != null ? result : Map.of("status", "deleted");
    }

    private void updateLocalProductFromPublication(Product product, Map<String, Object> mlFields) {
        boolean changed = false;


        if (mlFields.containsKey("title")) {
            String newTitle = (String) mlFields.get("title");
            if (newTitle != null && !newTitle.equals(product.getProductName())) {
                product.setProductName(newTitle);
                changed = true;
            }
        }


        if (mlFields.containsKey("price")) {
            Object priceObj = mlFields.get("price");
            BigDecimal newPrice = null;
            if (priceObj instanceof Number) {
                newPrice = BigDecimal.valueOf(((Number) priceObj).doubleValue());
            } else if (priceObj instanceof String) {
                try {
                    newPrice = new BigDecimal((String) priceObj);
                } catch (Exception ignored) {
                }
            }

            if (newPrice != null && (product.getPrice() == null || newPrice.compareTo(product.getPrice()) != 0)) {
                product.setPrice(newPrice);
                changed = true;
            }
        }


        if (mlFields.containsKey("available_quantity")) {
            Object stockObj = mlFields.get("available_quantity");
            BigDecimal newStock = null;
            if (stockObj instanceof Number) {
                newStock = BigDecimal.valueOf(((Number) stockObj).doubleValue());
            }

            if (newStock != null
                    && (product.getStockQuantity() == null || newStock.compareTo(product.getStockQuantity()) != 0)) {
                product.setStockQuantity(newStock);
                changed = true;
            }
        }

        if (changed) {
            productRepository.save(product);






        }
    }

    public Map<String, Object> getPublishedProduct(Long localProductId) {
        String accessToken = getAccessTokenRefreshed();
        Product localProduct = productRepository.findById(localProductId)
                .orElseThrow(() -> new RuntimeException("Local product not found: " + localProductId));
        if (localProduct.getMercadoLibreId() == null) {
            throw new RuntimeException("Product is not published to MercadoLibre");
        }

        String url = "https://api.mercadolibre.com/items/" + localProduct.getMercadoLibreId();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                });
        return response.getBody();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Map<String, Object>> searchCategories(String query) {
        String accessToken = getAccessTokenRefreshed();


        String url = "https://api.mercadolibre.com/sites/MLA/category_predictor/predict?title="
                + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            if (query.length() > 5 && query.toUpperCase().matches("MLA\\d+")) {
                String catUrl = "https://api.mercadolibre.com/categories/" + query.toUpperCase();
                try {
                    ResponseEntity<Map> catResponse = restTemplate.getForEntity(catUrl, Map.class);
                    Map<String, Object> catBody = catResponse.getBody();
                    if (catBody != null) {
                        Map<String, Object> cat = new HashMap<>();
                        cat.put("category_id", catBody.get("id"));
                        cat.put("category_name", catBody.get("name"));


                        List<Map<String, Object>> pathRoot = (List<Map<String, Object>>) catBody.get("path_from_root");
                        if (pathRoot != null) {
                            String fullPath = pathRoot.stream()
                                    .map(p -> (String) p.get("name"))
                                    .collect(Collectors.joining(" > "));
                            cat.put("path_from_root", fullPath);
                            cat.put("domain_name", fullPath);
                        } else {
                            cat.put("domain_name", catBody.get("name"));
                        }

                        List<Map<String, Object>> directResult = new ArrayList<>();
                        directResult.add(cat);
                        return directResult;
                    }
                } catch (Exception e) {
                    if (!e.getMessage().contains("404")) {
                        logger.warn("Direct category lookup failed for {}: {}", query, e.getMessage());
                    }
                }
            }

            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            List<Map<String, Object>> results = new ArrayList<>();
            if (response.getBody() != null) {
                for (Object item : response.getBody()) {
                    if (item instanceof Map) {
                        Map<String, Object> raw = (Map<String, Object>) item;
                        Map<String, Object> cat = new HashMap<>();
                        cat.put("category_id", raw.get("id"));
                        cat.put("category_name", raw.get("name"));


                        List<Map<String, Object>> pathRoot = (List<Map<String, Object>>) raw.get("path_from_root");
                        if (pathRoot != null && !pathRoot.isEmpty()) {
                            String fullPath = pathRoot.stream()
                                    .map(p -> (String) p.get("name"))
                                    .collect(Collectors.joining(" > "));
                            cat.put("path_from_root", fullPath);
                        } else {
                            cat.put("path_from_root", raw.get("name"));
                        }

                        cat.put("domain_name", cat.get("path_from_root"));
                        results.add(cat);
                    }
                }
            }
            return results;
        } catch (Exception e) {
            logger.error("Error searching ML categories: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Map<String, Object>> getListingTypes(String categoryId) {
        String accessToken = getAccessTokenRefreshed();
        MercadoLibreConfig config = getConfig();
        if (config == null || config.getUserId() == null) {
            return new ArrayList<>();
        }

        String url = "https://api.mercadolibre.com/users/" + config.getUserId()
                + "/available_listing_types?category_id=" + categoryId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            List<Map<String, Object>> result = new ArrayList<>();
            if (response.getBody() != null) {
                for (Object item : response.getBody()) {
                    if (item instanceof Map) {
                        Map<String, Object> raw = (Map<String, Object>) item;

                        Object available = raw.get("available");
                        if (Boolean.TRUE.equals(available)) {
                            Map<String, Object> lt = new HashMap<>();
                            lt.put("id", raw.get("id"));
                            lt.put("name", getListingTypeName(String.valueOf(raw.get("id"))));
                            result.add(lt);
                        }
                    }
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Error fetching listing types for category {}: {}", categoryId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Map<String, Object>> getCategoryAttributes(String categoryId) {
        String accessToken = getAccessTokenRefreshed();
        String url = "https://api.mercadolibre.com/categories/" + categoryId + "/attributes";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            logger.error("Error fetching attributes for category {}: {}", categoryId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<Map<String, Object>> getCategories(String categoryId) {
        String accessToken = getAccessTokenRefreshed();
        String url;
        boolean isRoot = categoryId == null || categoryId.isEmpty() || "root".equals(categoryId);
        if (isRoot) {
            url = "https://api.mercadolibre.com/sites/MLA/categories";
        } else {
            url = "https://api.mercadolibre.com/categories/" + categoryId;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            if (isRoot) {
                ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
                return response.getBody();
            } else {
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("children_categories")) {
                    return (List<Map<String, Object>>) body.get("children_categories");
                }
                return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("Error fetching ML categories: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String getListingTypeName(String listingTypeId) {

        switch (listingTypeId) {
            case "gold_pro":
                return "Premium";
            case "gold_special":
                return "Clásica";
            case "gold_premium":
                return "Oro Premium";
            case "gold":
                return "Oro";
            case "silver":
                return "Plata";
            case "bronze":
                return "Bronce";
            case "free":
                return "Gratuita";
            default:
                return listingTypeId;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMlErrorMessage(org.springframework.web.client.HttpClientErrorException e) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> errorBody = mapper.readValue(e.getResponseBodyAsString(), Map.class);
            String message = (String) errorBody.get("message");


            List<Map<String, Object>> causes = (List<Map<String, Object>>) errorBody.get("cause");
            if (causes != null && !causes.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Map<String, Object> cause : causes) {
                    String causeMsg = (String) cause.get("message");
                    if (causeMsg != null) {
                        if (sb.length() > 0)
                            sb.append("; ");
                        sb.append(causeMsg);
                    }
                }
                if (sb.length() > 0)
                    return sb.toString();
            }
            return message != null ? message : e.getMessage();
        } catch (Exception parseEx) {
            return e.getResponseBodyAsString();
        }
    }
}
