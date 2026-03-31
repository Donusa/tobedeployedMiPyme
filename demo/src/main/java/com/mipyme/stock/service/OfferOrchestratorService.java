package com.mipyme.stock.service;

import com.mipyme.stock.dto.GateCheckResult;
import com.mipyme.stock.dto.OfferActionResult;
import com.mipyme.stock.model.*;
import com.mipyme.stock.repository.EffectivePriceRepository;
import com.mipyme.stock.repository.OfferAuditLogRepository;
import com.mipyme.stock.repository.OfferRepository;
import com.mipyme.tiendanube.service.TiendaNubeService;
import com.mipyme.mercadolibre.service.MercadoLibreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OfferOrchestratorService {

    private final OfferRepository offerRepository;
    private final ChannelGateService gateService;
    private final OfferPricingService pricingService;
    private final TiendaNubeService tiendaNubeService;
    private final MercadoLibreService mercadoLibreService;
    private final EffectivePriceRepository effectivePriceRepository;
    private final OfferAuditLogRepository auditLogRepository;

    public OfferOrchestratorService(OfferRepository offerRepository,
            ChannelGateService gateService,
            OfferPricingService pricingService,
            TiendaNubeService tiendaNubeService,
            MercadoLibreService mercadoLibreService,
            EffectivePriceRepository effectivePriceRepository,
            OfferAuditLogRepository auditLogRepository) {
        this.offerRepository = offerRepository;
        this.gateService = gateService;
        this.pricingService = pricingService;
        this.tiendaNubeService = tiendaNubeService;
        this.mercadoLibreService = mercadoLibreService;
        this.effectivePriceRepository = effectivePriceRepository;
        this.auditLogRepository = auditLogRepository;
    }



    @Transactional
    public void deleteWithCleanup(Long offerId) {
        Offer offer = offerRepository.findById(offerId).orElse(null);
        if (offer == null) {
            return;
        }


        if (Boolean.TRUE.equals(offer.isPublishedTiendaNube())) {
            try {
                System.out.println("[OFFER-DELETE] Removing offer " + offerId + " from TiendaNube before delete...");
                removeFromTiendaNube(offer);
            } catch (Exception e) {
                System.out.println("[OFFER-DELETE] Error removing from TN (continuing with delete): " + e.getMessage());
            }
        }


        if (Boolean.TRUE.equals(offer.isPublishedMercadoLibre())) {
            try {
                System.out.println("[OFFER-DELETE] Removing offer " + offerId + " from MercadoLibre before delete...");
                removeFromMercadoLibre(offer);
            } catch (Exception e) {
                System.out.println("[OFFER-DELETE] Error removing from ML (continuing with delete): " + e.getMessage());
            }
        }


        effectivePriceRepository.deleteBySourceOfferId(offerId);

        logAudit(offer, null, "DELETED", null, null);
        offerRepository.delete(offer);
        System.out.println("[OFFER-DELETE] Offer " + offerId + " deleted successfully");
    }



    @Transactional
    public OfferActionResult applyToChannel(Long offerId, Channel channel) {
        Offer offer = offerRepository.findById(offerId).orElse(null);
        if (offer == null) {
            return OfferActionResult.fail(offerId, channel, "Oferta no encontrada");
        }

        GateCheckResult gate = gateService.canApply(offer, channel);
        if (!gate.isOk()) {
            return OfferActionResult.fail(offerId, channel, gate.getReason());
        }

        try {
            if (channel == Channel.TN) {
                return applyToTiendaNube(offer);
            } else if (channel == Channel.ML) {
                return applyToMercadoLibre(offer);
            }
            return OfferActionResult.fail(offerId, channel, "Canal no soportado");
        } catch (Exception e) {
            offer.setStatus(OfferStatus.ERROR);
            offer.setErrorMessage(e.getMessage());
            offerRepository.save(offer);
            logAudit(offer, channel, "APPLY_ERROR", null, e.getMessage());
            return OfferActionResult.fail(offerId, channel, "Error al publicar: " + e.getMessage());
        }
    }

    private OfferActionResult applyToTiendaNube(Offer offer) {
        System.out.println("[OFFER-SYNC] === applyToTiendaNube START === offerId=" + offer.getOfferId()
                + ", discountType=" + offer.getDiscountType() + ", discountValue=" + offer.getDiscountValue());

        var config = tiendaNubeService.getConfig();
        if (config == null) {
            System.out.println("[OFFER-SYNC] ERROR: TiendaNube config is null");
            return OfferActionResult.fail(offer.getOfferId(), Channel.TN, "TiendaNube no está conectada");
        }
        Long storeId = config.getUserId();
        System.out.println("[OFFER-SYNC] storeId=" + storeId);

        List<Product> products = gateService.resolveTargetProducts(offer);
        System.out.println("[OFFER-SYNC] Resolved " + products.size() + " target products");

        int successCount = 0;
        int skipCount = 0;
        StringBuilder errors = new StringBuilder();

        for (Product product : products) {
            System.out.println("[OFFER-SYNC] Product id=" + product.getProductId()
                    + ", name=" + product.getProductName()
                    + ", tnId=" + product.getTiendaNubeId()
                    + ", variants=" + (product.getProductVariants() != null ? product.getProductVariants().size() : 0));

            if (product.getTiendaNubeId() == null) {
                System.out.println("[OFFER-SYNC]   SKIP: product has no TiendaNube ID");
                skipCount++;
                continue;
            }

            if (product.getProductVariants() != null && !product.getProductVariants().isEmpty()) {

                for (ProductVariant variant : product.getProductVariants()) {
                    System.out.println("[OFFER-SYNC]   Variant variantId=" + variant.getProductVariantId()
                            + ", tnId=" + variant.getTiendaNubeId()
                            + ", price=" + variant.getPrice());

                    if (variant.getTiendaNubeId() == null) {
                        System.out.println("[OFFER-SYNC]     SKIP: variant has no TiendaNube ID");
                        continue;
                    }

                    try {
                        BigDecimal basePrice = variant.getPrice() != null ? variant.getPrice() : product.getPrice();
                        System.out.println("[OFFER-SYNC]     basePrice=" + basePrice);

                        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
                            System.out.println("[OFFER-SYNC]     SKIP: basePrice is null or <= 0");
                            skipCount++;
                            continue;
                        }

                        OfferPricingService.PriceResult priceResult = pricingService.computeEffectivePrice(basePrice,
                                offer);
                        if (priceResult == null) {
                            System.out.println("[OFFER-SYNC]     SKIP: priceResult is null (X_FOR_Y)");
                            skipCount++;
                            continue;
                        }

                        Map<String, Object> payload = new HashMap<>();
                        payload.put("promotional_price", priceResult.effectivePrice().toPlainString());
                        System.out.println("[OFFER-SYNC]     Sending PUT to TN: storeId=" + storeId
                                + ", productTnId=" + product.getTiendaNubeId()
                                + ", variantTnId=" + variant.getTiendaNubeId()
                                + ", payload=" + payload);

                        tiendaNubeService.updateVariant(
                                storeId,
                                product.getTiendaNubeId(),
                                variant.getTiendaNubeId(),
                                payload);

                        System.out.println("[OFFER-SYNC]     SUCCESS: variant updated on TN");
                        saveEffectivePrice(product.getProductId(), Channel.TN, priceResult.effectivePrice(), basePrice,
                                offer.getOfferId());
                        successCount++;
                    } catch (Exception e) {
                        System.out.println("[OFFER-SYNC]     ERROR: " + e.getMessage());
                        e.printStackTrace();
                        errors.append("Variante ").append(variant.getTiendaNubeId()).append(": ").append(e.getMessage())
                                .append("; ");
                    }
                }
            } else {

                System.out.println("[OFFER-SYNC]   Simple product (no local variants), fetching TN product...");
                try {
                    var tnProduct = tiendaNubeService.getProduct(product.getTiendaNubeId());
                    if (tnProduct != null && tnProduct.getVariants() != null) {
                        BigDecimal basePrice = product.getPrice();
                        System.out.println("[OFFER-SYNC]   basePrice=" + basePrice
                                + ", TN variants=" + tnProduct.getVariants().size());

                        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
                            System.out.println("[OFFER-SYNC]   SKIP: basePrice is null or <= 0");
                            skipCount++;
                        } else {
                            OfferPricingService.PriceResult priceResult = pricingService.computeEffectivePrice(
                                    basePrice, offer);
                            if (priceResult == null) {
                                System.out.println("[OFFER-SYNC]   SKIP: priceResult is null (X_FOR_Y)");
                                skipCount++;
                            } else {
                                for (var tnVariant : tnProduct.getVariants()) {
                                    Map<String, Object> payload = new HashMap<>();
                                    payload.put("promotional_price",
                                            priceResult.effectivePrice().toPlainString());
                                    System.out.println("[OFFER-SYNC]     Sending PUT to TN (simple): variantTnId="
                                            + tnVariant.getId() + ", payload=" + payload);

                                    tiendaNubeService.updateVariant(
                                            storeId,
                                            product.getTiendaNubeId(),
                                            tnVariant.getId(),
                                            payload);

                                    System.out.println("[OFFER-SYNC]     SUCCESS: TN variant updated");
                                    successCount++;
                                }
                                saveEffectivePrice(product.getProductId(), Channel.TN,
                                        priceResult.effectivePrice(), basePrice, offer.getOfferId());
                            }
                        }
                    } else {
                        System.out.println("[OFFER-SYNC]   SKIP: could not fetch TN product or has no variants");
                        skipCount++;
                    }
                } catch (Exception e) {
                    System.out.println("[OFFER-SYNC]   ERROR fetching/updating simple product: " + e.getMessage());
                    e.printStackTrace();
                    errors.append("Producto ").append(product.getTiendaNubeId()).append(": ").append(e.getMessage())
                            .append("; ");
                }
            }
        }

        System.out.println("[OFFER-SYNC] === RESULT: success=" + successCount + ", skip=" + skipCount
                + ", errors=" + errors);

        offer.setPublishedTiendaNube(true);
        offer.setStatus(errors.isEmpty() ? OfferStatus.ACTIVE : OfferStatus.ERROR);
        offer.setErrorMessage(errors.isEmpty() ? null : errors.toString());
        offer.setLastSyncedAt(LocalDateTime.now());
        offerRepository.save(offer);

        logAudit(offer, Channel.TN, "APPLIED", "ok=" + successCount + ",skip=" + skipCount, errors.toString());

        String message = "Publicado en TiendaNube: " + successCount + " variante(s) actualizadas";
        if (skipCount > 0)
            message += ", " + skipCount + " omitidas";
        if (!errors.isEmpty())
            message += ". Errores: " + errors;

        return OfferActionResult.ok(offer.getOfferId(), Channel.TN, offer.getStatus(), message);
    }

    private OfferActionResult applyToMercadoLibre(Offer offer) {
        List<Product> products = gateService.resolveTargetProducts(offer);
        int successCount = 0;
        int skipCount = 0;
        StringBuilder errors = new StringBuilder();

        for (Product product : products) {
            if (product.getMercadoLibreId() == null) {
                skipCount++;
                continue;
            }

            try {
                BigDecimal basePrice = product.getPrice();
                if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
                    skipCount++;
                    continue;
                }

                OfferPricingService.PriceResult priceResult = pricingService.computeEffectivePrice(basePrice, offer);
                if (priceResult == null) {
                    skipCount++;
                    continue;
                }

                mercadoLibreService.applySalePrice(product.getMercadoLibreId(), priceResult.effectivePrice());
                saveEffectivePrice(product.getProductId(), Channel.ML, priceResult.effectivePrice(), basePrice,
                        offer.getOfferId());
                successCount++;
            } catch (Exception e) {
                errors.append("Item ").append(product.getMercadoLibreId()).append(": ").append(e.getMessage())
                        .append("; ");
            }
        }

        offer.setPublishedMercadoLibre(true);
        offer.setStatus(errors.isEmpty() ? OfferStatus.ACTIVE : OfferStatus.ERROR);
        offer.setErrorMessage(errors.isEmpty() ? null : errors.toString());
        offer.setLastSyncedAt(LocalDateTime.now());
        offerRepository.save(offer);

        logAudit(offer, Channel.ML, "APPLIED", "ok=" + successCount + ",skip=" + skipCount, errors.toString());

        String message = "Publicado en MercadoLibre: " + successCount + " item(s) actualizados";
        if (skipCount > 0)
            message += ", " + skipCount + " omitidos";
        if (!errors.isEmpty())
            message += ". Errores: " + errors;

        return OfferActionResult.ok(offer.getOfferId(), Channel.ML, offer.getStatus(), message);
    }



    @Transactional
    public OfferActionResult removeFromChannel(Long offerId, Channel channel) {
        Offer offer = offerRepository.findById(offerId).orElse(null);
        if (offer == null) {
            return OfferActionResult.fail(offerId, channel, "Oferta no encontrada");
        }

        GateCheckResult gate = gateService.canRemove(offer, channel);
        if (!gate.isOk()) {
            return OfferActionResult.fail(offerId, channel, gate.getReason());
        }

        try {
            if (channel == Channel.TN) {
                return removeFromTiendaNube(offer);
            } else if (channel == Channel.ML) {
                return removeFromMercadoLibre(offer);
            }
            return OfferActionResult.fail(offerId, channel, "Canal no soportado");
        } catch (Exception e) {
            offer.setStatus(OfferStatus.ERROR);
            offer.setErrorMessage(e.getMessage());
            offerRepository.save(offer);
            logAudit(offer, channel, "REMOVE_ERROR", null, e.getMessage());
            return OfferActionResult.fail(offerId, channel, "Error al quitar: " + e.getMessage());
        }
    }

    private OfferActionResult removeFromTiendaNube(Offer offer) {
        var config = tiendaNubeService.getConfig();
        if (config == null) {
            return OfferActionResult.fail(offer.getOfferId(), Channel.TN, "TiendaNube no está conectada");
        }
        Long storeId = config.getUserId();

        List<Product> products = gateService.resolveTargetProducts(offer);
        int successCount = 0;
        StringBuilder errors = new StringBuilder();

        for (Product product : products) {
            if (product.getTiendaNubeId() == null)
                continue;

            if (product.getProductVariants() != null && !product.getProductVariants().isEmpty()) {
                for (ProductVariant variant : product.getProductVariants()) {
                    if (variant.getTiendaNubeId() == null)
                        continue;

                    try {
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("promotional_price", "");

                        tiendaNubeService.updateVariant(
                                storeId,
                                product.getTiendaNubeId(),
                                variant.getTiendaNubeId(),
                                payload);
                        successCount++;
                    } catch (Exception e) {
                        errors.append("Variante ").append(variant.getTiendaNubeId()).append(": ").append(e.getMessage())
                                .append("; ");
                    }
                }
            } else {

                try {
                    var tnProduct = tiendaNubeService.getProduct(product.getTiendaNubeId());
                    if (tnProduct != null && tnProduct.getVariants() != null) {
                        for (var tnVariant : tnProduct.getVariants()) {
                            Map<String, Object> payload = new HashMap<>();
                            payload.put("promotional_price", "");
                            tiendaNubeService.updateVariant(
                                    storeId,
                                    product.getTiendaNubeId(),
                                    tnVariant.getId(),
                                    payload);
                            successCount++;
                        }
                    }
                } catch (Exception e) {
                    errors.append("Producto ").append(product.getTiendaNubeId()).append(": ").append(e.getMessage())
                            .append("; ");
                }
            }

            effectivePriceRepository.deleteByLocalProductIdAndChannel(product.getProductId(), Channel.TN);
        }

        offer.setPublishedTiendaNube(false);
        offer.setStatus(OfferStatus.REMOVED);
        offer.setErrorMessage(errors.isEmpty() ? null : errors.toString());
        offer.setLastSyncedAt(LocalDateTime.now());
        offerRepository.save(offer);

        logAudit(offer, Channel.TN, "REMOVED", "ok=" + successCount, errors.toString());

        return OfferActionResult.ok(offer.getOfferId(), Channel.TN, OfferStatus.REMOVED,
                "Despublicado de TiendaNube: " + successCount + " variante(s) restauradas");
    }

    private OfferActionResult removeFromMercadoLibre(Offer offer) {
        List<Product> products = gateService.resolveTargetProducts(offer);
        int successCount = 0;
        StringBuilder errors = new StringBuilder();

        for (Product product : products) {
            if (product.getMercadoLibreId() == null)
                continue;

            try {
                BigDecimal originalPrice = product.getPrice();
                if (originalPrice == null)
                    continue;

                EffectivePrice ep = effectivePriceRepository
                        .findByLocalProductIdAndChannel(product.getProductId(), Channel.ML).orElse(null);
                BigDecimal restorePrice = ep != null ? ep.getRegularPrice() : originalPrice;

                mercadoLibreService.removeSalePrice(product.getMercadoLibreId(), restorePrice);
                effectivePriceRepository.deleteByLocalProductIdAndChannel(product.getProductId(), Channel.ML);
                successCount++;
            } catch (Exception e) {
                errors.append("Item ").append(product.getMercadoLibreId()).append(": ").append(e.getMessage())
                        .append("; ");
            }
        }

        offer.setPublishedMercadoLibre(false);
        offer.setStatus(OfferStatus.REMOVED);
        offer.setErrorMessage(errors.isEmpty() ? null : errors.toString());
        offer.setLastSyncedAt(LocalDateTime.now());
        offerRepository.save(offer);

        logAudit(offer, Channel.ML, "REMOVED", "ok=" + successCount, errors.toString());

        return OfferActionResult.ok(offer.getOfferId(), Channel.ML, OfferStatus.REMOVED,
                "Despublicado de MercadoLibre: " + successCount + " item(s) restaurados");
    }



    @Transactional
    public OfferActionResult syncChannel(Long offerId, Channel channel) {
        Offer offer = offerRepository.findById(offerId).orElse(null);
        if (offer == null) {
            return OfferActionResult.fail(offerId, channel, "Oferta no encontrada");
        }

        GateCheckResult gate = gateService.canSync(offer, channel);
        if (!gate.isOk()) {
            return OfferActionResult.fail(offerId, channel, gate.getReason());
        }

        try {
            if (channel == Channel.TN) {
                return applyToTiendaNube(offer);
            } else if (channel == Channel.ML) {
                return applyToMercadoLibre(offer);
            }
            return OfferActionResult.fail(offerId, channel, "Canal no soportado");
        } catch (Exception e) {
            offer.setStatus(OfferStatus.ERROR);
            offer.setErrorMessage(e.getMessage());
            offerRepository.save(offer);
            logAudit(offer, channel, "SYNC_ERROR", null, e.getMessage());
            return OfferActionResult.fail(offerId, channel, "Error al sincronizar: " + e.getMessage());
        }
    }



    public GateCheckResult checkGate(Long offerId, Channel channel) {
        Offer offer = offerRepository.findById(offerId).orElse(null);
        if (offer == null) {
            return GateCheckResult.block("Oferta no encontrada");
        }
        return gateService.canApply(offer, channel);
    }



    private void saveEffectivePrice(Long productId, Channel channel, BigDecimal effectivePrice, BigDecimal regularPrice,
            Long offerId) {
        EffectivePrice ep = effectivePriceRepository.findByLocalProductIdAndChannel(productId, channel).orElse(null);
        if (ep == null) {
            ep = new EffectivePrice();
            ep.setLocalProductId(productId);
            ep.setChannel(channel);
        }
        ep.setEffectivePrice(effectivePrice);
        ep.setRegularPrice(regularPrice);
        ep.setSourceOfferId(offerId);
        ep.setComputedAt(LocalDateTime.now());
        effectivePriceRepository.save(ep);
    }

    private void logAudit(Offer offer, Channel channel, String action, String requestData, String responseData) {
        try {
            OfferAuditLog log = new OfferAuditLog();
            log.setOfferId(offer.getOfferId());
            log.setChannel(channel);
            log.setAction(action);
            log.setRequestData(requestData);
            log.setResponseData(responseData);
            log.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("Failed to save audit log: " + e.getMessage());
        }
    }
}
