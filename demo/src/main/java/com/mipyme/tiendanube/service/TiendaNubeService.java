package com.mipyme.tiendanube.service;

import com.mipyme.tiendanube.dto.TokenResponse;
import com.mipyme.tiendanube.dto.TiendaNubeOrderSummaryDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mipyme.tiendanube.dto.TiendaNubeProduct;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import com.mipyme.tiendanube.model.TiendaNubeConfig;
import com.mipyme.tiendanube.repository.TiendaNubeConfigRepository;
import com.mipyme.company.CompanySchemaService;
import com.mipyme.tenant.TenantContext;

import com.mipyme.stock.model.Product;
import com.mipyme.stock.model.ProductVariant;
import com.mipyme.stock.repository.ProductRepository;
import com.mipyme.stock.repository.ProductVariantRepository;
import com.mipyme.tiendanube.dto.SyncPreviewDTO;
import com.mipyme.tiendanube.dto.SyncDifference;
import com.mipyme.tiendanube.dto.TiendaNubeVariant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mipyme.notification.NotificationService;
import com.mipyme.notification.model.Notification.NotificationType;

@Service
public class TiendaNubeService {

    private static final Logger logger = LoggerFactory.getLogger(TiendaNubeService.class);

    @Value("${tiendanube.app-id:}")
    private String appId;

    @Value("${tiendanube.client-secret:}")
    private String clientSecret;

    @Value("${tiendanube.user-agent:MiPyme/1.0 (contacto@mipyme.com)}")
    private String userAgent;

    @Value("${tiendanube.webhook-base-url:}")
    private String webhookBaseUrl;

    private final RestTemplate restTemplate;
    private final TiendaNubeConfigRepository configRepository;
    private final CompanySchemaService companySchemaService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final com.mipyme.orders.repository.LocalOrderTrackingRepository localOrderTrackingRepository;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    private final NotificationService notificationService;

    public TiendaNubeService(TiendaNubeConfigRepository configRepository, CompanySchemaService companySchemaService,
            ProductRepository productRepository, ProductVariantRepository productVariantRepository,
            com.mipyme.orders.repository.LocalOrderTrackingRepository localOrderTrackingRepository,
            org.springframework.transaction.PlatformTransactionManager transactionManager,
            NotificationService notificationService) {
        this.configRepository = configRepository;
        this.companySchemaService = companySchemaService;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.localOrderTrackingRepository = localOrderTrackingRepository;
        this.transactionTemplate = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        this.notificationService = notificationService;


        this.restTemplate = new RestTemplate(new JdkClientHttpRequestFactory());
    }

    private final java.util.Set<String> usedCodes = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public TokenResponse exchangeToken(String code, String redirectUri) {

        if (code == null || usedCodes.contains(code)) {
            logger.error(
                    "Security Alert: Code reuse detected or null code! Code: " + (code == null ? "NULL" : "REUSED"));
            throw new RuntimeException("Authorization code already used or invalid.");
        }
        usedCodes.add(code);






        logger.info("Starting fresh TiendaNube sync. Clearing existing data...");
        try {

            transactionTemplate.execute(status -> {
                unlinkStore();
                return null;
            });
            logger.info("Existing TiendaNube data cleared successfully.");
        } catch (Exception e) {
            logger.error("Error clearing existing data (continuing anyway): " + e.getMessage());
        }


        String currentSchema = TenantContext.getCurrentTenant();
        if (currentSchema != null) {
            companySchemaService.createTiendaNubeConfigTable(currentSchema);
        }

        String url = "https://www.tiendanube.com/apps/authorize/token";

        logger.info("Preparing token exchange request to URL: " + url);
        String maskedCode = (code != null && code.length() > 8)
                ? code.substring(0, 4) + "..." + code.substring(code.length() - 4)
                : "INVALID_CODE";
        logger.info("Exchanging code: " + maskedCode);
        logger.info("Using Client ID: " + appId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", userAgent);

        Map<String, String> body = new HashMap<>();
        body.put("client_id", appId);
        body.put("client_secret", clientSecret);
        body.put("grant_type", "authorization_code");
        body.put("code", code);








        Map<String, String> logBody = new HashMap<>(body);
        logBody.put("client_secret", "********");
        logger.info("Request Body: " + logBody);
        logger.info("Request Headers: " + headers);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            logger.info("Sending token exchange request to TN...");
            ResponseEntity<String> rawResponse = restTemplate.postForEntity(url, request, String.class);
            logger.info("TN Token Response: " + rawResponse.getStatusCode() + " Body: " + rawResponse.getBody());

            ObjectMapper mapper = new ObjectMapper();
            TokenResponse tokenResponse = mapper.readValue(rawResponse.getBody(), TokenResponse.class);

            if (tokenResponse.getError() != null) {
                logger.error("TiendaNube returned error: " + tokenResponse.getError() + " - "
                        + tokenResponse.getErrorDescription());
                throw new RuntimeException("TiendaNube Authentication Error: " + tokenResponse.getErrorDescription());
            }


            saveToken(tokenResponse);
            logger.info("Token saved successfully for user: " + tokenResponse.getUserId());


            try {
                registerWebhooks(tokenResponse.getUserId(), tokenResponse.getAccessToken());
            } catch (Exception e) {
                logger.error("Error registering webhooks (non-fatal): " + e.getMessage());
            }

            return tokenResponse;
        } catch (Exception e) {
            logger.error("Error retrieving token from TiendaNube. Request details: URL=" + url + ", ClientID=" + appId,
                    e);
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
            }
            throw new RuntimeException("Failed to exchange token: " + e.getMessage(), e);
        }
    }

    private void saveToken(TokenResponse tokenResponse) {
        TiendaNubeConfig config = configRepository.findTopByOrderByIdDesc().orElse(new TiendaNubeConfig());
        config.setAccessToken(tokenResponse.getAccessToken());
        config.setTokenType(tokenResponse.getTokenType());
        config.setScope(tokenResponse.getScope());
        config.setUserId(tokenResponse.getUserId());
        configRepository.save(config);
    }

    public TiendaNubeConfig getConfig() {
        return configRepository.findTopByOrderByIdDesc().orElse(null);
    }

    private void registerWebhooks(Long storeId, String accessToken) {
        String baseUrl = webhookBaseUrl + "/api/tiendanube/webhooks";
        String[] events = { "order/created", "order/updated", "order/paid", "order/packed", "order/fulfilled",
                "order/cancelled" };

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + accessToken);
        headers.set("Content-Type", "application/json");
        headers.set("User-Agent", userAgent);

        String url = "https://api.tiendanube.com/v1/" + storeId + "/webhooks";

        for (String event : events) {
            try {
                Map<String, String> body = new HashMap<>();
                body.put("event", event);
                String suffix = event.replace("/", "-");
                body.put("url", baseUrl + "/" + suffix);

                HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
                restTemplate.postForEntity(url, request, String.class);
                System.out.println("Registered webhook for " + event);
            } catch (Exception e) {


                System.err.println("Warning registering webhook for " + event + ": " + e.getMessage());
            }
        }
    }

    public List<TiendaNubeOrderSummaryDTO> getOrders() {
        TiendaNubeConfig config = getConfig();
        if (config == null || config.getAccessToken() == null) {
            System.out.println("No TiendaNube access token found. Returning empty list (Graceful degradation).");
            return new ArrayList<>();
        }


        String url = "https://api.tiendanube.com/v1/" + config.getUserId() + "/orders?per_page=50";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + config.getAccessToken());
        headers.set("User-Agent", userAgent);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<TiendaNubeOrderSummaryDTO[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, TiendaNubeOrderSummaryDTO[].class);
            if (response.getBody() != null) {
                List<TiendaNubeOrderSummaryDTO> orders = Arrays.asList(response.getBody());

                List<com.mipyme.orders.model.LocalOrderTracking> localTrackings = new ArrayList<>();
                try {
                    if (!orders.isEmpty()) {
                        List<String> orderIds = orders.stream()
                                .map(o -> String.valueOf(o.getId()))
                                .collect(Collectors.toList());

                        localTrackings = localOrderTrackingRepository
                                .findByExternalIdInAndSource(orderIds, "tiendanube");
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching local tracking for TN orders: " + e.getMessage());
                }

                System.out.println("Fetched " + orders.size() + " orders from TiendaNube. Found "
                        + localTrackings.size() + " local overrides.");


                for (TiendaNubeOrderSummaryDTO order : orders) {
                    String apiStatus = order.getStatus();
                    String orderIdStr = String.valueOf(order.getId());

                    java.util.Optional<com.mipyme.orders.model.LocalOrderTracking> overrideOpt = localTrackings.stream()
                            .filter(lt -> lt.getExternalId().equals(orderIdStr))
                            .findFirst();

                    if (overrideOpt.isPresent()) {
                        String localStatus = overrideOpt.get().getLocalStatus();
                        System.out.println("Order " + order.getId() + ": API Status = " + apiStatus
                                + ", Local Status = " + localStatus);

                        if (localStatus != null && !localStatus.equalsIgnoreCase(apiStatus)) {
                            System.out.println("Applying local override for Order " + order.getId() + ": " + apiStatus
                                    + " -> " + localStatus);
                            order.setStatus(localStatus);
                        }
                    } else {


                    }
                }


                for (TiendaNubeOrderSummaryDTO order : orders) {
                    try {
                        String refKey = "tn-order-" + order.getId();
                        String msg = "Orden TiendaNube #"
                                + (order.getNumber() != null ? order.getNumber() : order.getId())
                                + " - $" + (order.getTotal() != null ? order.getTotal() : "0");
                        notificationService.createIfNotExists(
                                NotificationType.TN_WEBHOOK, msg, "/tiendanube", refKey);
                    } catch (Exception e) {

                    }
                }

                return orders;
            }
        } catch (HttpClientErrorException.NotFound e) {
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error fetching TiendaNube orders (Graceful degradation): " + e.getMessage());

            return new ArrayList<>();
        }
        return new ArrayList<>();
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateLocalTracking(String orderId, String status) {
        java.util.Optional<com.mipyme.orders.model.LocalOrderTracking> existing = localOrderTrackingRepository
                .findByExternalIdAndSource(orderId, "tiendanube");
        com.mipyme.orders.model.LocalOrderTracking tracking;
        if (existing.isPresent()) {
            tracking = existing.get();
        } else {
            tracking = new com.mipyme.orders.model.LocalOrderTracking();
            tracking.setExternalId(orderId);
            tracking.setSource("tiendanube");
        }
        tracking.setLocalStatus(status);
        tracking.setUpdatedAt(java.time.LocalDateTime.now());
        localOrderTrackingRepository.save(tracking);
        System.out.println("Updated local tracking for TN order " + orderId + " to " + status);
    }

    public List<TiendaNubeProduct> getProducts(Long storeId, String accessToken) {
        String tokenToUse = accessToken;
        Long storeIdToUse = storeId;

        if (tokenToUse == null || storeIdToUse == null) {
            TiendaNubeConfig config = getConfig();
            if (config != null) {
                if (tokenToUse == null)
                    tokenToUse = config.getAccessToken();
                if (storeIdToUse == null)
                    storeIdToUse = config.getUserId();
            }
        }

        if (tokenToUse == null || storeIdToUse == null) {
            throw new RuntimeException("No TiendaNube access token available.");
        }

        String url = "https://api.tiendanube.com/v1/" + storeIdToUse + "/products";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + tokenToUse);
        headers.set("User-Agent", userAgent);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<TiendaNubeProduct[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    TiendaNubeProduct[].class);
            return Arrays.asList(response.getBody());
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching products from TiendaNube: " + e.getResponseBodyAsString());
            throw e;
        }
    }

    public TiendaNubeProduct getProduct(Long id) {
        TiendaNubeConfig config = getConfig();
        if (config == null || config.getAccessToken() == null) {
            throw new RuntimeException("No TiendaNube access token available.");
        }

        String url = "https://api.tiendanube.com/v1/" + config.getUserId() + "/products/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + config.getAccessToken());
        headers.set("User-Agent", userAgent);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<TiendaNubeProduct> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    TiendaNubeProduct.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.err
                    .println("Error fetching product from TiendaNube (ID: " + id + "): " + e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 403) {
                throw new RuntimeException(
                        "TiendaNube denied access to product " + id + ". Check scopes or product ownership.");
            }
            throw e;
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<SyncPreviewDTO> previewSync(List<Long> tnProductIds) {




        List<TiendaNubeProduct> allTnProducts = getProducts(null, null);
        List<TiendaNubeProduct> selectedTnProducts = allTnProducts.stream()
                .filter(p -> tnProductIds.contains(p.getId()))
                .collect(Collectors.toList());

        List<SyncPreviewDTO> previews = new ArrayList<>();

        for (TiendaNubeProduct tnP : selectedTnProducts) {
            List<Product> localList = productRepository.findByTiendaNubeId(tnP.getId());
            Product local = null;

            if (!localList.isEmpty()) {
                if (localList.size() > 1) {
                    System.err.println(
                            "Warning: Multiple local products found for TN ID " + tnP.getId() + ". Using first one.");
                }
                local = localList.get(0);
            }

            if (local == null) {
                String name = getMappedName(tnP);
                if (name != null) {
                    localList = productRepository.findByProductName(name);
                    if (!localList.isEmpty()) {
                        if (localList.size() > 1) {
                            System.err.println(
                                    "Warning: Multiple local products found for name '" + name + "'. Using first one.");
                        }
                        local = localList.get(0);
                    }
                }
            }
            previews.add(createPreview(tnP, local, null));
        }
        return previews;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public SyncPreviewDTO previewManualMatch(Long tnProductId, Long localProductId) {
        TiendaNubeProduct tnP = getProduct(tnProductId);
        Product local = productRepository.findByIdWithVariants(localProductId)
                .orElseThrow(() -> new RuntimeException("Local product not found: " + localProductId));

        return createPreview(tnP, local, null);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<SyncPreviewDTO> previewBulkSync(List<com.mipyme.tiendanube.dto.ManualSyncRequest> requests) {
        List<SyncPreviewDTO> previews = new ArrayList<>();

        for (com.mipyme.tiendanube.dto.ManualSyncRequest req : requests) {
            try {
                if (req.isCreateNew()) {

                    TiendaNubeProduct tnP = getProduct(req.getTnProductId());
                    previews.add(createPreview(tnP, null, null));
                } else if (req.getLocalProductId() != null) {

                    TiendaNubeProduct tnP = getProduct(req.getTnProductId());
                    Product local = productRepository.findByIdWithVariants(req.getLocalProductId())
                            .orElseThrow(
                                    () -> new RuntimeException("Local product not found: " + req.getLocalProductId()));
                    previews.add(createPreview(tnP, local, req.getVariantMapping()));
                } else {

                    previews.addAll(previewSync(java.util.Collections.singletonList(req.getTnProductId())));
                }
            } catch (Exception e) {
                System.err.println(
                        "Error processing bulk sync item (TN ID: " + req.getTnProductId() + "): " + e.getMessage());


            }
        }

        return previews;
    }

    private String getMappedName(TiendaNubeProduct tnP) {
        if (tnP.getName() == null)
            return null;
        String name = tnP.getName().get("es");
        if (name == null && !tnP.getName().isEmpty()) {
            name = tnP.getName().values().iterator().next();
        }
        return name;
    }

    private String formatDimensions(BigDecimal w, BigDecimal h, BigDecimal d) {
        return (w != null ? w : "0") + "x" + (h != null ? h : "0") + "x" + (d != null ? d : "0");
    }

    private SyncPreviewDTO createPreview(TiendaNubeProduct tnP, Product local, Map<Long, Long> variantMapping) {
        SyncPreviewDTO dto = new SyncPreviewDTO(tnP, local);

        if (local != null) {

            String tnMappedName = getMappedName(tnP);


            if (tnMappedName != null && !tnMappedName.equals(local.getProductName())) {
                SyncDifference diff = new SyncDifference("name", "Product", null, local.getProductName(), tnMappedName);
                diff.setLabel("Nombre");
                dto.addDifference(diff);
            }

            boolean isLocalSimple = local.getProductVariants() == null || local.getProductVariants().isEmpty();
            boolean isRemoteSingleVariant = tnP.getVariants() != null && tnP.getVariants().size() == 1;
            boolean isRemoteMultiVariant = tnP.getVariants() != null && tnP.getVariants().size() > 1;

            if (isLocalSimple && isRemoteSingleVariant) {

                TiendaNubeVariant tnV = tnP.getVariants().get(0);


                if (tnV.getPrice() != null) {
                    BigDecimal localPrice = local.getPrice() != null ? local.getPrice() : BigDecimal.ZERO;
                    if (tnV.getPrice().compareTo(localPrice) != 0) {
                        SyncDifference diff = new SyncDifference("price", "Product", null,
                                localPrice.toString(),
                                tnV.getPrice().toString());
                        diff.setLabel("Precio");
                        dto.addDifference(diff);
                    }
                }


                if (tnV.getStock() != null) {
                    BigDecimal localStock = local.getStockQuantity() != null ? local.getStockQuantity()
                            : BigDecimal.ZERO;
                    BigDecimal tnStock = new BigDecimal(tnV.getStock());
                    if (tnStock.compareTo(localStock) != 0) {
                        SyncDifference diff = new SyncDifference("stock", "Product", null,
                                localStock.toString(),
                                tnStock.toString());
                        diff.setLabel("Stock");
                        dto.addDifference(diff);
                    }
                }


                if (tnV.getSku() != null) {
                    String localSku = local.getInternalCode() != null ? local.getInternalCode() : "";
                    if (!tnV.getSku().equals(localSku)) {
                        SyncDifference diff = new SyncDifference("sku", "Product", null, localSku, tnV.getSku());
                        diff.setLabel("SKU / Código Interno");
                        dto.addDifference(diff);
                    }
                }







            } else if (isLocalSimple && isRemoteMultiVariant) {

                SyncDifference diff = new SyncDifference("type", "Product", null, "Simple", "Variants");
                diff.setLabel("Tipo de Producto");
                dto.addDifference(diff);

            } else if (tnP.getVariants() != null && !tnP.getVariants().isEmpty()) {


                for (TiendaNubeVariant tnV : tnP.getVariants()) {

                    ProductVariant localV = findMatchingLocalVariant(tnV, local, variantMapping);

                    if (localV != null) {

                        if (localV.getTiendaNubeId() == null || !localV.getTiendaNubeId().equals(tnV.getId())) {
                            String variantName = "Variante " + tnV.getId();
                            if (tnV.getValues() != null) {
                                variantName = tnV.getValues().stream()
                                        .map(m -> m.get("es"))
                                        .filter(s -> s != null)
                                        .collect(Collectors.joining(" / "));
                            }
                            SyncDifference linkDiff = new SyncDifference("variant_mapping", "Variant",
                                    String.valueOf(tnV.getId()), String.valueOf(localV.getProductVariantId()),
                                    variantName);
                            linkDiff.setResolution("LOCAL");
                            linkDiff.setLabel("Vinculación de Variante");
                            dto.addDifference(linkDiff);
                        }


                        if (tnV.getPrice() != null) {
                            BigDecimal localPrice = localV.getPrice() != null ? localV.getPrice() : BigDecimal.ZERO;
                            if (tnV.getPrice().compareTo(localPrice) != 0) {
                                SyncDifference diff = new SyncDifference("price", "Variant",
                                        String.valueOf(tnV.getId()),
                                        localPrice.toString(),
                                        tnV.getPrice().toString());
                                diff.setLabel("Precio");
                                dto.addDifference(diff);
                            }
                        }


                        if (tnV.getStock() != null) {
                            BigDecimal tnStock = new BigDecimal(tnV.getStock());
                            BigDecimal localStock = localV.getStockQuantity() != null ? localV.getStockQuantity()
                                    : BigDecimal.ZERO;
                            if (tnStock.compareTo(localStock) != 0) {
                                SyncDifference diff = new SyncDifference("stock", "Variant",
                                        String.valueOf(tnV.getId()),
                                        localStock.toString(),
                                        tnStock.toString());
                                diff.setLabel("Stock");
                                dto.addDifference(diff);
                            }
                        }


                        if (tnV.getBarcode() != null) {
                            String localGtin = localV.getVariantGtin() != null ? localV.getVariantGtin() : "";
                            if (!tnV.getBarcode().equals(localGtin)) {
                                SyncDifference diff = new SyncDifference("barcode", "Variant",
                                        String.valueOf(tnV.getId()),
                                        localGtin,
                                        tnV.getBarcode());
                                diff.setLabel("Código de Barras");
                                dto.addDifference(diff);
                            }
                        }


                        if (tnV.getWeight() != null) {
                            BigDecimal localWeight = localV.getNetWeightGrams() != null ? localV.getNetWeightGrams()
                                    : BigDecimal.ZERO;
                            if (tnV.getWeight().compareTo(localWeight) != 0) {
                                SyncDifference diff = new SyncDifference("weight", "Variant",
                                        String.valueOf(tnV.getId()),
                                        localWeight.toString(),
                                        tnV.getWeight().toString());
                                diff.setLabel("Peso (g)");
                                dto.addDifference(diff);
                            }
                        }


                        String tnDimensions = formatDimensions(tnV.getWidth(), tnV.getHeight(), tnV.getDepth());
                        String localDimensions = localV.getSizeDimensionsCm() != null ? localV.getSizeDimensionsCm()
                                : "0x0x0";
                        if (!tnDimensions.equals(localDimensions)) {
                            SyncDifference diff = new SyncDifference("dimensions", "Variant",
                                    String.valueOf(tnV.getId()),
                                    localDimensions,
                                    tnDimensions);
                            diff.setLabel("Dimensiones (AxAxP)");
                            dto.addDifference(diff);
                        }


                        if (tnV.getSku() != null) {
                            String localSku = localV.getVariantSku() != null ? localV.getVariantSku() : "";
                            if (!tnV.getSku().equals(localSku)) {
                                SyncDifference diff = new SyncDifference("sku", "Variant", String.valueOf(tnV.getId()),
                                        localSku,
                                        tnV.getSku());
                                diff.setLabel("SKU");
                                dto.addDifference(diff);
                            }
                        }

                    } else {

                        String variantName = "Variante sin nombre";
                        if (tnV.getValues() != null) {
                            variantName = tnV.getValues().stream()
                                    .map(m -> m.get("es"))
                                    .filter(s -> s != null)
                                    .collect(Collectors.joining(" / "));
                        }





                        SyncDifference diff = new SyncDifference("variant_mapping", "Variant", tnV.getSku(), null,
                                variantName);

                        diff.setIdentifier(String.valueOf(tnV.getId()));
                        diff.setLabel("Vinculación de Variante");
                        dto.addDifference(diff);
                    }
                }
            }
        }
        return dto;
    }

    private ProductVariant findMatchingLocalVariant(TiendaNubeVariant tnV, Product local,
            Map<Long, Long> variantMapping) {
        if (local.getProductVariants() == null || local.getProductVariants().isEmpty()) {
            return null;
        }


        if (variantMapping != null && variantMapping.containsKey(tnV.getId())) {
            Long localId = variantMapping.get(tnV.getId());


            if (localId != null && localId == -1L) {
                return null;
            }

            if (localId != null) {
                return local.getProductVariants().stream()
                        .filter(v -> v.getProductVariantId().equals(localId))
                        .findFirst()
                        .orElse(null);
            }
        }


        Optional<ProductVariant> byId = local.getProductVariants().stream()
                .filter(v -> v.getTiendaNubeId() != null && v.getTiendaNubeId().equals(tnV.getId()))
                .findFirst();
        if (byId.isPresent())
            return byId.get();


        if (tnV.getSku() != null) {
            Optional<ProductVariant> bySku = local.getProductVariants().stream()
                    .filter(v -> v.getVariantSku() != null && v.getVariantSku().equals(tnV.getSku()))
                    .findFirst();
            if (bySku.isPresent())
                return bySku.get();
        }



        if (local.getProductVariants().size() == 1) {





            return local.getProductVariants().get(0);
        }

        return null;
    }

    @org.springframework.transaction.annotation.Transactional
    public void executeSync(List<SyncPreviewDTO> resolvedPreviews) {
        System.out.println("Executing sync for " + resolvedPreviews.size() + " previews.");
        for (SyncPreviewDTO preview : resolvedPreviews) {
            try {
                if (preview.isNew()) {
                    System.out.println(
                            "Creating new local product from TN Product ID: " + preview.getTiendaNubeProduct().getId());
                    createLocalProductFromTiendaNube(preview.getTiendaNubeProduct());
                } else {
                    Long localId = preview.getLocalProduct().getId();
                    System.out.println("Syncing existing local product ID: " + localId);
                    Product localProduct = productRepository.findByIdWithVariants(localId).orElse(null);

                    if (localProduct != null) {
                        if (localProduct.getTiendaNubeId() == null) {
                            localProduct.setTiendaNubeId(preview.getTiendaNubeProduct().getId());
                            productRepository.save(localProduct);
                            System.out.println("Linked local product " + localId + " to TN "
                                    + preview.getTiendaNubeProduct().getId());
                        }


                        List<SyncDifference> diffs = preview.getDifferences();
                        diffs.sort((d1, d2) -> {
                            if ("variant_mapping".equals(d1.getField()) && !"variant_mapping".equals(d2.getField()))
                                return -1;
                            if (!"variant_mapping".equals(d1.getField()) && "variant_mapping".equals(d2.getField()))
                                return 1;
                            return 0;
                        });

                        for (SyncDifference diff : diffs) {
                            if (diff.getResolution() != null) {
                                System.out.println("Applying resolution " + diff.getResolution() + " for field "
                                        + diff.getField() + " on " + diff.getLevel());
                                applyResolution(localProduct, preview.getTiendaNubeProduct(), diff);
                            } else {
                                System.out.println("Skipping diff " + diff.getField() + " (No resolution selected)");
                            }
                        }
                    } else {
                        System.err.println("Local product not found: " + localId);
                    }
                }
            } catch (Exception e) {
                System.err.println(
                        "Error syncing product " + preview.getTiendaNubeProduct().getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void createLocalProductFromTiendaNube(TiendaNubeProduct tnP) {
        Product p = new Product();
        p.setTiendaNubeId(tnP.getId());

        String name = tnP.getName().get("es");
        if (name == null && !tnP.getName().isEmpty())
            name = tnP.getName().values().iterator().next();
        p.setProductName(name);


        boolean isSimple = tnP.getVariants() != null && tnP.getVariants().size() == 1;

        if (isSimple) {
            TiendaNubeVariant tnV = tnP.getVariants().get(0);
            p.setPrice(tnV.getPrice());
            if (tnV.getStock() != null) {
                p.setStockQuantity(new BigDecimal(tnV.getStock()));
            }
            p.setInternalCode(tnV.getSku());
        }

        productRepository.save(p);

        if (!isSimple && tnP.getVariants() != null) {
            for (TiendaNubeVariant tnV : tnP.getVariants()) {
                ProductVariant v = new ProductVariant();
                v.setProduct(p);
                v.setTiendaNubeId(tnV.getId());
                v.setVariantSku(tnV.getSku());
                v.setPrice(tnV.getPrice());
                if (tnV.getStock() != null) {
                    v.setStockQuantity(new BigDecimal(tnV.getStock()));
                }
                productVariantRepository.save(v);
            }
        }
    }

    private void applyResolution(Product local, TiendaNubeProduct remote, SyncDifference diff) {

        if ("Variant".equals(diff.getLevel()) && "variant_mapping".equals(diff.getField())) {

            try {
                Long tnVariantId = Long.valueOf(diff.getIdentifier());

                String localVal = diff.getLocalValue();

                if (localVal != null && !localVal.equals("null")) {
                    Long localVariantId = Long.valueOf(localVal);
                    ProductVariant localV = productVariantRepository.findById(localVariantId).orElse(null);
                    if (localV != null) {

                        if (!localV.getProduct().getProductId().equals(local.getProductId())) {
                            System.err.println("Security Warning: Attempted to link variant " + localVariantId +
                                    " (Product " + localV.getProduct().getProductId() + ") to Product "
                                    + local.getProductId());
                            return;
                        }

                        localV.setTiendaNubeId(tnVariantId);
                        productVariantRepository.save(localV);
                        System.out.println("Mapped local variant " + localVariantId + " to TN variant " + tnVariantId);



                        if (local.getProductVariants() != null) {
                            local.getProductVariants().stream()
                                    .filter(v -> v.getProductVariantId().equals(localV.getProductVariantId()))
                                    .findFirst()
                                    .ifPresent(v -> v.setTiendaNubeId(tnVariantId));
                        }
                    } else {
                        System.err.println("Local variant not found for mapping: " + localVariantId);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error mapping variant: " + e.getMessage());
            }
            return;
        }

        if ("REMOTE".equals(diff.getResolution())) {
            if ("Product".equals(diff.getLevel())) {
                boolean updated = false;
                if ("name".equals(diff.getField())) {
                    local.setProductName(diff.getRemoteValue());
                    updated = true;
                } else if ("price".equals(diff.getField())) {
                    if (diff.getRemoteValue() != null) {
                        local.setPrice(new BigDecimal(diff.getRemoteValue()));
                        updated = true;
                    }
                } else if ("stock".equals(diff.getField())) {
                    if (diff.getRemoteValue() != null) {
                        local.setStockQuantity(new BigDecimal(diff.getRemoteValue()));
                        updated = true;
                    }
                } else if ("sku".equals(diff.getField())) {
                    local.setInternalCode(diff.getRemoteValue());
                    updated = true;
                } else if ("type".equals(diff.getField())) {
                    transformProductToVariants(local, remote);

                }

                if (updated) {
                    productRepository.save(local);
                    System.out.println("Updated local product " + local.getProductId() + " field " + diff.getField());
                }
            } else if ("Variant".equals(diff.getLevel())) {
                Long tnVariantId = Long.valueOf(diff.getIdentifier());
                Optional<ProductVariant> vOpt = Optional.empty();


                if (local.getProductVariants() != null) {
                    vOpt = local.getProductVariants().stream()
                            .filter(v -> v.getTiendaNubeId() != null && v.getTiendaNubeId().equals(tnVariantId))
                            .findFirst();
                }


                if (vOpt.isEmpty() && remote.getVariants() != null) {
                    String sku = remote.getVariants().stream()
                            .filter(v -> v.getId().equals(tnVariantId))
                            .map(TiendaNubeVariant::getSku)
                            .findFirst().orElse(null);

                    if (sku != null && local.getProductVariants() != null) {
                        vOpt = local.getProductVariants().stream()
                                .filter(v -> sku.equals(v.getVariantSku()))
                                .findFirst();
                    }
                }

                if (vOpt.isPresent()) {
                    ProductVariant v = vOpt.get();
                    boolean vUpdated = false;
                    if ("price".equals(diff.getField()) && diff.getRemoteValue() != null) {
                        v.setPrice(new BigDecimal(diff.getRemoteValue()));
                        vUpdated = true;
                    } else if ("stock".equals(diff.getField()) && diff.getRemoteValue() != null) {
                        v.setStockQuantity(new BigDecimal(diff.getRemoteValue()));
                        vUpdated = true;
                    } else if ("barcode".equals(diff.getField())) {
                        v.setVariantGtin(diff.getRemoteValue());
                        vUpdated = true;
                    } else if ("weight".equals(diff.getField()) && diff.getRemoteValue() != null) {
                        v.setNetWeightGrams(new BigDecimal(diff.getRemoteValue()));
                        vUpdated = true;
                    } else if ("dimensions".equals(diff.getField())) {
                        v.setSizeDimensionsCm(diff.getRemoteValue());
                        vUpdated = true;
                    } else if ("sku".equals(diff.getField())) {
                        v.setVariantSku(diff.getRemoteValue());
                        vUpdated = true;
                    }

                    if (vUpdated) {
                        productVariantRepository.save(v);
                        System.out.println(
                                "Updated local variant " + v.getProductVariantId() + " field " + diff.getField());
                    }
                } else {
                    System.err.println("Could not find local variant for TN variant " + tnVariantId);
                }
            }
        } else if ("LOCAL".equals(diff.getResolution())) {
            updateTiendaNubeEntity(remote, diff);
        }
    }

    private void transformProductToVariants(Product local, TiendaNubeProduct remote) {







        if (remote.getVariants() != null) {
            for (TiendaNubeVariant tnV : remote.getVariants()) {
                ProductVariant v = new ProductVariant();
                v.setProduct(local);
                v.setTiendaNubeId(tnV.getId());
                v.setVariantSku(tnV.getSku());
                v.setPrice(tnV.getPrice());
                if (tnV.getStock() != null) {
                    v.setStockQuantity(new BigDecimal(tnV.getStock()));
                }
                productVariantRepository.save(v);
            }
        }



    }

    private void updateTiendaNubeEntity(TiendaNubeProduct remote, SyncDifference diff) {
        TiendaNubeConfig config = getConfig();
        if (config == null || config.getAccessToken() == null)
            return;


        if (diff.getLocalValue() == null && !"variant_mapping".equals(diff.getField())) {
            System.out.println("Skipping TN update for field " + diff.getField() + " because local value is null");
            return;
        }

        String baseUrl = "https://api.tiendanube.com/v1/" + config.getUserId();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + config.getAccessToken());
        headers.set("User-Agent", userAgent);
        headers.setContentType(MediaType.APPLICATION_JSON);

        if ("Product".equals(diff.getLevel())) {
            if ("name".equals(diff.getField())) {
                String url = baseUrl + "/products/" + remote.getId();
                Map<String, Object> body = new HashMap<>();
                Map<String, String> nameMap = new HashMap<>();
                nameMap.put("es", diff.getLocalValue());
                body.put("name", nameMap);

                sendUpdateRequest(url, body, headers);
            } else if ("price".equals(diff.getField()) || "stock".equals(diff.getField())) {

                if (remote.getVariants() != null && !remote.getVariants().isEmpty()) {
                    TiendaNubeVariant mainVariant = remote.getVariants().get(0);
                    String url = baseUrl + "/products/" + remote.getId() + "/variants/" + mainVariant.getId();
                    Map<String, Object> body = new HashMap<>();

                    if ("price".equals(diff.getField())) {
                        body.put("price", diff.getLocalValue());
                    } else if ("stock".equals(diff.getField())) {
                        try {
                            body.put("stock", Integer.parseInt(diff.getLocalValue()));
                        } catch (Exception e) {
                            body.put("stock", 0);
                        }
                    } else if ("sku".equals(diff.getField())) {
                        body.put("sku", diff.getLocalValue());
                    }
                    sendUpdateRequest(url, body, headers);
                }
            }
        } else if ("Variant".equals(diff.getLevel())) {

            if ("variant_mapping".equals(diff.getField())) {
                return;
            }

            Long variantId = null;
            try {
                variantId = Long.valueOf(diff.getIdentifier());
            } catch (NumberFormatException e) {


                if (remote.getVariants() != null) {
                    for (TiendaNubeVariant v : remote.getVariants()) {
                        if (diff.getIdentifier() != null && diff.getIdentifier().equals(v.getSku())) {
                            variantId = v.getId();
                            break;
                        }
                    }
                }
            }

            if (variantId != null) {

                boolean exists = false;
                if (remote.getVariants() != null) {
                    for (TiendaNubeVariant v : remote.getVariants()) {
                        if (v.getId().equals(variantId)) {
                            exists = true;
                            break;
                        }
                    }
                }

                if (exists) {
                    String url = baseUrl + "/products/" + remote.getId() + "/variants/" + variantId;
                    Map<String, Object> body = new HashMap<>();
                    boolean hasUpdate = false;

                    if ("price".equals(diff.getField())) {
                        body.put("price", diff.getLocalValue());
                        hasUpdate = true;
                    } else if ("stock".equals(diff.getField())) {
                        try {
                            body.put("stock", Integer.parseInt(diff.getLocalValue()));
                            hasUpdate = true;
                        } catch (Exception e) {
                            body.put("stock", 0);
                            hasUpdate = true;
                        }
                    } else if ("barcode".equals(diff.getField())) {
                        body.put("barcode", diff.getLocalValue());
                        hasUpdate = true;
                    } else if ("weight".equals(diff.getField())) {
                        body.put("weight", diff.getLocalValue());
                        hasUpdate = true;
                    } else if ("dimensions".equals(diff.getField())) {

                        try {
                            String[] parts = diff.getLocalValue().split("x");
                            if (parts.length == 3) {
                                body.put("width", parts[0]);
                                body.put("height", parts[1]);
                                body.put("depth", parts[2]);
                                hasUpdate = true;
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing dimensions for TN update: " + diff.getLocalValue());
                        }
                    } else if ("sku".equals(diff.getField())) {
                        body.put("sku", diff.getLocalValue());
                        hasUpdate = true;
                    }

                    if (hasUpdate) {
                        sendUpdateRequest(url, body, headers);
                    } else {
                        System.out.println("Skipping empty update for TN variant " + variantId);
                    }
                }
            }
        }
    }

    private void sendUpdateRequest(String url, Map<String, Object> body, HttpHeaders headers) {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        } catch (Exception e) {
            System.err.println("Failed to update TiendaNube entity: " + e.getMessage());
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Product> getLinkedLocalProducts() {
        return productRepository.findByTiendaNubeIdIsNotNull();
    }

    @org.springframework.transaction.annotation.Transactional
    public void unlinkProducts(List<Long> localProductIds) {
        List<Product> products = productRepository.findAllById(localProductIds);
        for (Product p : products) {

            p.setTiendaNubeId(null);
            productRepository.save(p);


            if (p.getProductVariants() != null) {
                for (ProductVariant v : p.getProductVariants()) {
                    v.setTiendaNubeId(null);
                    productVariantRepository.save(v);
                }
            }
            System.out.println("Unlinked local product " + p.getProductId());
        }
    }

    public void unlinkStore() {
        logger.info("Unlinking TiendaNube store...");
        int productsUnlinked = productRepository.unlinkAllTiendaNubeProducts();
        logger.info("Unlinked " + productsUnlinked + " products.");

        int variantsUnlinked = productVariantRepository.unlinkAllTiendaNubeVariants();
        logger.info("Unlinked " + variantsUnlinked + " variants.");

        long configsDeleted = configRepository.count();
        configRepository.deleteAll();
        logger.info("Deleted " + configsDeleted + " config records.");

        logger.info("TiendaNube store unlinked successfully.");
    }

    public List<Map<String, Object>> getFulfillmentOrders(Long orderId) {
        TiendaNubeConfig config = configRepository.findTopByOrderByIdDesc().orElse(null);
        if (config == null || config.getAccessToken() == null) {
            System.err.println("TiendaNube config missing or no token.");
            return new ArrayList<>();
        }

        String url = "https://api.tiendanube.com/v1/" + config.getUserId() + "/orders/" + orderId
                + "/fulfillment-orders";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + config.getAccessToken());
        headers.set("User-Agent", userAgent);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            System.out.println("No fulfillments found for TiendaNube order " + orderId
                    + " (404). This is normal if order is new or has no shipments.");
            return new ArrayList<>();
        } catch (HttpClientErrorException.Unauthorized e) {
            System.err.println("Unauthorized accessing TiendaNube order " + orderId
                    + ". Token might be expired. (Graceful degradation)");
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error fetching fulfillments for order " + orderId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Map<String, Object> updateFulfillmentOrder(Long orderId, String fulfillmentId, Map<String, Object> data) {
        TiendaNubeConfig config = configRepository.findTopByOrderByIdDesc().orElse(null);
        if (config == null || config.getAccessToken() == null) {
            System.err.println("TiendaNube config missing or no token.");
            return null;
        }

        String url = "https://api.tiendanube.com/v1/" + config.getUserId() + "/orders/" + orderId
                + "/fulfillment-orders/"
                + fulfillmentId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + config.getAccessToken());
        headers.set("User-Agent", userAgent);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);
        try {

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });



            String status = (String) data.get("status");
            if (status != null) {
                com.mipyme.orders.model.LocalOrderTracking tracking = localOrderTrackingRepository
                        .findByExternalIdAndSource(String.valueOf(orderId), "tiendanube")
                        .orElse(new com.mipyme.orders.model.LocalOrderTracking());

                if (tracking.getId() == null) {
                    tracking.setExternalId(String.valueOf(orderId));
                    tracking.setSource("tiendanube");
                }
                tracking.setLocalStatus(status);
                tracking.setUpdatedAt(java.time.LocalDateTime.now());
                localOrderTrackingRepository.save(tracking);
            }

            return response.getBody();
        } catch (Exception e) {
            System.err.println(
                    "Error updating fulfillment " + fulfillmentId + " for order " + orderId + ": " + e.getMessage());
            throw e;
        }
    }

    public void closeOrder(Long orderId) {
        TiendaNubeConfig config = configRepository.findTopByOrderByIdDesc().orElse(null);
        if (config == null || config.getAccessToken() == null) {
            return;
        }

        String url = "https://api.tiendanube.com/v1/" + config.getUserId() + "/orders/" + orderId + "/close";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + config.getAccessToken());
        headers.set("User-Agent", userAgent);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(new HashMap<>(), headers);
        try {
            restTemplate.postForEntity(url, entity, Void.class);
        } catch (Exception e) {
            System.err.println("Error closing order: " + e.getMessage());
        }
    }

    public void markOrderAsPaid(Long orderId) {
        System.out.println("TiendaNubeService: Attempting to mark order " + orderId + " as paid.");
        TiendaNubeConfig config = configRepository.findTopByOrderByIdDesc().orElse(null);
        if (config == null || config.getAccessToken() == null) {
            throw new RuntimeException("No TiendaNube access token available");
        }

        String url = "https://api.tiendanube.com/v1/" + config.getUserId() + "/orders/" + orderId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + config.getAccessToken());
        headers.set("User-Agent", userAgent);
        headers.setContentType(MediaType.APPLICATION_JSON);


        try {
            java.util.Optional<com.mipyme.orders.model.LocalOrderTracking> existing = localOrderTrackingRepository
                    .findByExternalIdAndSource(String.valueOf(orderId), "tiendanube");
            com.mipyme.orders.model.LocalOrderTracking tracking;
            if (existing.isPresent()) {
                tracking = existing.get();
            } else {
                tracking = new com.mipyme.orders.model.LocalOrderTracking();
                tracking.setExternalId(String.valueOf(orderId));
                tracking.setSource("tiendanube");

                tracking.setLocalStatus("open");
            }
            tracking.setPaymentStatus("paid");
            tracking.setUpdatedAt(java.time.LocalDateTime.now());
            localOrderTrackingRepository.save(tracking);
            System.out.println("Locally marked TiendaNube order " + orderId + " as paid.");
        } catch (Exception e) {
            System.err.println("Error saving local payment status: " + e.getMessage());
            e.printStackTrace();
        }


        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Map<String, Object>> getResponse = restTemplate.exchange(url, HttpMethod.GET, requestEntity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });
            Map<String, Object> orderData = getResponse.getBody();

            if (orderData != null && orderData.containsKey("total")) {
                Object totalObj = orderData.get("total");
                String total = (totalObj != null) ? totalObj.toString() : "0.00";

                Map<String, Object> body = new HashMap<>();
                body.put("payment_status", "paid");
                body.put("paid_at",
                        java.time.OffsetDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
                body.put("total_paid", total);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
                System.out.println("Sent payment update to TiendaNube API for order " + orderId);
            }
        } catch (Exception e) {
            System.err.println("Error calling TiendaNube API for order " + orderId
                    + " (Ignored as local status is saved): " + e.getMessage());

        }
    }




    public TiendaNubeVariant getVariant(Long storeId, Long productId, Long variantId) {
        TiendaNubeConfig config = getConfig();
        if (config == null || config.getAccessToken() == null) {
            throw new RuntimeException("No TiendaNube access token available");
        }

        String url = "https://api.tiendanube.com/v1/" + storeId + "/products/" + productId + "/variants/" + variantId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + config.getAccessToken());
        headers.set("User-Agent", userAgent);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<TiendaNubeVariant> response = restTemplate.exchange(
                url, HttpMethod.GET, request, TiendaNubeVariant.class);
        return response.getBody();
    }


    public void updateVariant(Long storeId, Long productId, Long variantId, Map<String, Object> payload) {
        TiendaNubeConfig config = getConfig();
        if (config == null || config.getAccessToken() == null) {
            throw new RuntimeException("No TiendaNube access token available");
        }

        String url = "https://api.tiendanube.com/v1/" + storeId + "/products/" + productId + "/variants/" + variantId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + config.getAccessToken());
        headers.set("User-Agent", userAgent);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
    }


    public void registerDiscountCallback(String callbackUrl) {
        TiendaNubeConfig config = getConfig();
        if (config == null || config.getAccessToken() == null) {
            throw new RuntimeException("No TiendaNube access token available");
        }

        String url = "https://api.tiendanube.com/v1/" + config.getUserId() + "/discounts/callbacks";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + config.getAccessToken());
        headers.set("User-Agent", userAgent);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("url", callbackUrl);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(url, request, String.class);
            System.out.println("Registered discount callback: " + callbackUrl);
        } catch (HttpClientErrorException e) {

            if (e.getStatusCode().value() == 409 || e.getStatusCode().value() == 422) {
                restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
                System.out.println("Updated discount callback: " + callbackUrl);
            } else {
                throw e;
            }
        }
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map<String, Object> createPromotion(Map<String, Object> promotionData) {
        TiendaNubeConfig config = getConfig();
        if (config == null || config.getAccessToken() == null) {
            throw new RuntimeException("No TiendaNube access token available");
        }

        String url = "https://api.tiendanube.com/v1/" + config.getUserId() + "/promotions";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + config.getAccessToken());
        headers.set("User-Agent", userAgent);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(promotionData, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        return response.getBody();
    }


    public void deletePromotion(String promotionId) {
        TiendaNubeConfig config = getConfig();
        if (config == null || config.getAccessToken() == null) {
            throw new RuntimeException("No TiendaNube access token available");
        }

        String url = "https://api.tiendanube.com/v1/" + config.getUserId() + "/promotions/" + promotionId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + config.getAccessToken());
        headers.set("User-Agent", userAgent);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
    }



    @SuppressWarnings({ "unchecked", "rawtypes" })
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> publishProduct(Long localProductId, Map<String, Object> tnFields) {
        TiendaNubeConfig config = getConfig();
        if (config == null || config.getAccessToken() == null) {
            throw new RuntimeException("No TiendaNube access token available");
        }

        Product localProduct = productRepository.findByIdWithVariants(localProductId)
                .orElseThrow(() -> new RuntimeException("Local product not found: " + localProductId));

        if (localProduct.getTiendaNubeId() != null) {
            throw new RuntimeException(
                    "Product already published to TiendaNube (ID: " + localProduct.getTiendaNubeId() + ")");
        }

        Map<String, Object> body = new HashMap<>();
        if (tnFields.containsKey("name")) {
            body.put("name", tnFields.get("name"));
        } else {
            Map<String, String> nameMap = new HashMap<>();
            nameMap.put("es", localProduct.getProductName());
            body.put("name", nameMap);
        }
        if (tnFields.containsKey("description"))
            body.put("description", tnFields.get("description"));
        if (tnFields.containsKey("handle"))
            body.put("handle", tnFields.get("handle"));
        if (tnFields.containsKey("images"))
            body.put("images", tnFields.get("images"));


        List<Map<String, Object>> variants = new ArrayList<>();
        boolean hasVariants = localProduct.getProductVariants() != null && !localProduct.getProductVariants().isEmpty();
        if (hasVariants) {
            for (ProductVariant v : localProduct.getProductVariants()) {
                Map<String, Object> vb = new HashMap<>();
                if (v.getPrice() != null)
                    vb.put("price", v.getPrice().toString());
                if (v.getStockQuantity() != null)
                    vb.put("stock", v.getStockQuantity().intValue());
                if (v.getVariantSku() != null)
                    vb.put("sku", v.getVariantSku());
                if (v.getVariantGtin() != null)
                    vb.put("barcode", v.getVariantGtin());
                if (v.getNetWeightGrams() != null)
                    vb.put("weight", v.getNetWeightGrams().toString());
                variants.add(vb);
            }
        } else {
            Map<String, Object> vb = new HashMap<>();
            if (localProduct.getPrice() != null)
                vb.put("price", localProduct.getPrice().toString());
            if (localProduct.getStockQuantity() != null)
                vb.put("stock", localProduct.getStockQuantity().intValue());
            if (localProduct.getInternalCode() != null)
                vb.put("sku", localProduct.getInternalCode());
            variants.add(vb);
        }
        body.put("variants", variants);

        String url = "https://api.tiendanube.com/v1/" + config.getUserId() + "/products";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + config.getAccessToken());
        headers.set("User-Agent", userAgent);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, req, Map.class);
            Map<String, Object> created = response.getBody();
            if (created != null) {
                Number tnId = (Number) created.get("id");
                if (tnId != null) {
                    localProduct.setTiendaNubeId(tnId.longValue());
                    productRepository.save(localProduct);
                    List<Map<String, Object>> createdVars = (List<Map<String, Object>>) created.get("variants");
                    if (createdVars != null && hasVariants) {
                        List<ProductVariant> lv = localProduct.getProductVariants();
                        for (int i = 0; i < Math.min(lv.size(), createdVars.size()); i++) {
                            Number varId = (Number) createdVars.get(i).get("id");
                            if (varId != null) {
                                lv.get(i).setTiendaNubeId(varId.longValue());
                                productVariantRepository.save(lv.get(i));
                            }
                        }
                    }
                }
            }
            System.out.println("Published local product " + localProductId + " to TiendaNube");
            return created;
        } catch (HttpClientErrorException e) {
            System.err.println("Error publishing to TiendaNube: " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to publish to TiendaNube: " + e.getResponseBodyAsString(), e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> updatePublishedProduct(Long localProductId, Map<String, Object> tnFields) {
        TiendaNubeConfig config = getConfig();
        if (config == null || config.getAccessToken() == null) {
            throw new RuntimeException("No TiendaNube access token available");
        }
        Product localProduct = productRepository.findByIdWithVariants(localProductId)
                .orElseThrow(() -> new RuntimeException("Local product not found: " + localProductId));
        if (localProduct.getTiendaNubeId() == null) {
            throw new RuntimeException("Product is not published to TiendaNube");
        }

        Map<String, Object> body = new HashMap<>();
        if (tnFields.containsKey("name"))
            body.put("name", tnFields.get("name"));
        if (tnFields.containsKey("description"))
            body.put("description", tnFields.get("description"));
        if (tnFields.containsKey("handle"))
            body.put("handle", tnFields.get("handle"));
        if (tnFields.containsKey("images"))
            body.put("images", tnFields.get("images"));

        String url = "https://api.tiendanube.com/v1/" + config.getUserId() + "/products/"
                + localProduct.getTiendaNubeId();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authentication", "bearer " + config.getAccessToken());
        headers.set("User-Agent", userAgent);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, req, Map.class);
            System.out.println("Updated TiendaNube product " + localProduct.getTiendaNubeId());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            System.err.println("Error updating TiendaNube product: " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to update on TiendaNube: " + e.getResponseBodyAsString(), e);
        }
    }

    public TiendaNubeProduct getPublishedProduct(Long localProductId) {
        Product localProduct = productRepository.findById(localProductId)
                .orElseThrow(() -> new RuntimeException("Local product not found: " + localProductId));
        if (localProduct.getTiendaNubeId() == null) {
            throw new RuntimeException("Product is not published to TiendaNube");
        }
        return getProduct(localProduct.getTiendaNubeId());
    }
}