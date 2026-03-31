package com.mipyme.stock.service;

import com.mipyme.stock.dto.GateCheckResult;
import com.mipyme.stock.model.*;
import com.mipyme.stock.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ChannelGateService {

    private final ProductRepository productRepository;

    public ChannelGateService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public GateCheckResult canApply(Offer offer, Channel channel) {

        if (offer.getTargetType() == null || offer.getTargetIds() == null || offer.getTargetIds().isEmpty()) {
            return GateCheckResult.block("La oferta no tiene productos objetivo definidos");
        }


        if (offer.getDiscountType() == null || offer.getDiscountValue() == null) {
            return GateCheckResult.block("La oferta no tiene tipo o valor de descuento definido");
        }


        if (!Boolean.TRUE.equals(offer.isIndefinite())) {
            if (offer.getEndDate() != null && offer.getEndDate().isBefore(LocalDate.now())) {
                return GateCheckResult.block("La oferta ya venció (fecha fin: " + offer.getEndDate() + ")");
            }
        }


        if (offer.getDiscountType() == DiscountType.X_FOR_Y) {
            if (offer.getBuyQuantity() == null || offer.getPayQuantity() == null
                    || offer.getBuyQuantity() <= 0 || offer.getPayQuantity() <= 0) {
                return GateCheckResult.block("Oferta X por Y requiere cantidades de compra y pago válidas");
            }
            if (offer.getPayQuantity() >= offer.getBuyQuantity()) {
                return GateCheckResult.block("La cantidad de pago debe ser menor que la cantidad de compra");
            }
        }


        List<Product> products = resolveTargetProducts(offer);
        if (products.isEmpty()) {
            return GateCheckResult.block("No se encontraron productos para el objetivo seleccionado");
        }

        if (channel == Channel.TN) {
            return validateTnMapping(products, offer);
        } else if (channel == Channel.ML) {
            return validateMlMapping(products, offer);
        }

        return GateCheckResult.block("Canal no soportado");
    }

    public GateCheckResult canRemove(Offer offer, Channel channel) {
        if (offer.getStatus() != OfferStatus.ACTIVE && offer.getStatus() != OfferStatus.ERROR
                && offer.getStatus() != OfferStatus.OUT_OF_SYNC) {
            return GateCheckResult
                    .block("Solo se pueden despublicar ofertas con estado ACTIVE, ERROR u OUT_OF_SYNC (actual: "
                            + offer.getStatus() + ")");
        }
        return GateCheckResult.pass();
    }

    public GateCheckResult canSync(Offer offer, Channel channel) {
        if (offer.getTargetType() == null || offer.getTargetIds() == null || offer.getTargetIds().isEmpty()) {
            return GateCheckResult.block("La oferta no tiene productos objetivo definidos");
        }

        List<Product> products = resolveTargetProducts(offer);
        if (products.isEmpty()) {
            return GateCheckResult.block("No se encontraron productos para el objetivo seleccionado");
        }

        if (channel == Channel.TN) {
            long mappedCount = products.stream()
                    .filter(p -> p.getTiendaNubeId() != null)
                    .count();
            if (mappedCount == 0) {
                return GateCheckResult.block("Ningún producto está vinculado a TiendaNube");
            }
        } else if (channel == Channel.ML) {
            long mappedCount = products.stream()
                    .filter(p -> p.getMercadoLibreId() != null)
                    .count();
            if (mappedCount == 0) {
                return GateCheckResult.block("Ningún producto está vinculado a MercadoLibre");
            }
        }

        return GateCheckResult.pass();
    }

    public List<Product> resolveTargetProducts(Offer offer) {
        OfferTargetType type = offer.getTargetType();
        List<Long> ids = offer.getTargetIds();

        return switch (type) {
            case SPECIFIC -> productRepository.findByProductIdInWithVariants(ids);
            case CATEGORY -> productRepository.findByCategoryIdsWithVariants(ids);
            case BRAND -> productRepository.findByBrandIdsWithVariants(ids);
            case WAREHOUSE -> productRepository.findByWarehouseIdsWithVariants(ids);
            case LOCATION -> productRepository.findByStorageIdsWithVariants(ids);
        };
    }

    private GateCheckResult validateTnMapping(List<Product> products, Offer offer) {
        long unmappedCount = products.stream()
                .filter(p -> p.getTiendaNubeId() == null)
                .count();

        if (unmappedCount == products.size()) {
            return GateCheckResult.block("Ningún producto está vinculado a TiendaNube");
        }


        if (offer.getTnMode() == TnMode.VARIANT_PRICE || offer.getTnMode() == null) {
            long variantsWithoutTnId = products.stream()
                    .filter(p -> p.getTiendaNubeId() != null)
                    .flatMap(p -> p.getProductVariants().stream())
                    .filter(v -> v.getTiendaNubeId() == null)
                    .count();



            long totalVariantsOfMapped = products.stream()
                    .filter(p -> p.getTiendaNubeId() != null)
                    .flatMap(p -> p.getProductVariants().stream())
                    .count();

            if (totalVariantsOfMapped > 0 && variantsWithoutTnId == totalVariantsOfMapped) {
                return GateCheckResult.block("Las variantes de los productos vinculados no tienen ID de TiendaNube");
            }
        }


        if (offer.getDiscountType() != DiscountType.X_FOR_Y) {
            for (Product p : products) {
                if (p.getTiendaNubeId() != null && p.getPrice() != null) {
                    BigDecimal effective = computeQuickPrice(p.getPrice(), offer);
                    if (effective != null && effective.compareTo(BigDecimal.ZERO) <= 0) {
                        return GateCheckResult
                                .block("El precio final sería ≤ 0 para el producto \"" + p.getProductName() + "\"");
                    }
                }
            }
        }

        if (unmappedCount > 0) {

        }

        return GateCheckResult.pass();
    }

    private GateCheckResult validateMlMapping(List<Product> products, Offer offer) {
        long unmappedCount = products.stream()
                .filter(p -> p.getMercadoLibreId() == null)
                .count();

        if (unmappedCount == products.size()) {
            return GateCheckResult.block("Ningún producto está vinculado a MercadoLibre");
        }


        if (offer.getDiscountType() != DiscountType.X_FOR_Y) {
            for (Product p : products) {
                if (p.getMercadoLibreId() != null && p.getPrice() != null) {
                    BigDecimal effective = computeQuickPrice(p.getPrice(), offer);
                    if (effective != null && effective.compareTo(BigDecimal.ZERO) <= 0) {
                        return GateCheckResult
                                .block("El precio final sería ≤ 0 para el producto \"" + p.getProductName() + "\"");
                    }
                }
            }
        }

        return GateCheckResult.pass();
    }

    private BigDecimal computeQuickPrice(BigDecimal basePrice, Offer offer) {
        if (offer.getDiscountType() == DiscountType.PERCENTAGE) {
            return basePrice.multiply(BigDecimal.ONE.subtract(
                    offer.getDiscountValue().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)));
        } else if (offer.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            return basePrice.subtract(offer.getDiscountValue());
        }
        return null;
    }
}
