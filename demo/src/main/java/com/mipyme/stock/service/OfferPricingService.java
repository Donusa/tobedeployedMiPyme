package com.mipyme.stock.service;

import com.mipyme.stock.model.Offer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class OfferPricingService {

    public record PriceResult(BigDecimal effectivePrice, BigDecimal regularPrice) {
    }


    public PriceResult computeEffectivePrice(BigDecimal basePrice, Offer offer) {
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Precio base debe ser > 0");
        }
        if (offer.getDiscountType() == null || offer.getDiscountValue() == null) {
            throw new IllegalArgumentException("Tipo o valor de descuento no definido");
        }

        BigDecimal effective;

        switch (offer.getDiscountType()) {
            case PERCENTAGE -> {
                BigDecimal factor = BigDecimal.ONE.subtract(
                        offer.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                effective = basePrice.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            }
            case FIXED_AMOUNT -> {
                effective = basePrice.subtract(offer.getDiscountValue()).setScale(2, RoundingMode.HALF_UP);
            }
            case X_FOR_Y -> {

                return null;
            }
            default -> throw new IllegalArgumentException("Tipo de descuento desconocido: " + offer.getDiscountType());
        }

        if (effective.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio final calculado es ≤ 0 (" + effective + "). Precio base: "
                    + basePrice + ", descuento: " + offer.getDiscountValue());
        }

        if (effective.compareTo(basePrice) >= 0) {
            throw new IllegalArgumentException(
                    "El precio final (" + effective + ") no es menor al precio base (" + basePrice + ")");
        }

        return new PriceResult(effective, basePrice);
    }
}
