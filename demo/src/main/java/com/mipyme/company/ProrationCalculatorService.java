package com.mipyme.company;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;


@Service
public class ProrationCalculatorService {

    private static final Logger logger = LoggerFactory.getLogger(ProrationCalculatorService.class);
    private static final ZoneId AR_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final BigDecimal MIN_CHARGE = new BigDecimal("1.00");


    public record ProrationResult(BigDecimal differential, long daysRemaining, long daysInCycle) {
        public boolean requiresPayment() {
            return differential != null && differential.compareTo(BigDecimal.ZERO) > 0;
        }
    }


    public ProrationResult calculate(
            BigDecimal currentPlanPriceArs,
            BigDecimal newPlanPriceArs,
            String billingCycle,
            Instant validUntil,
            Instant currentPeriodStart) {

        if (currentPlanPriceArs == null || newPlanPriceArs == null) {
            throw new IllegalArgumentException("Plan prices must not be null");
        }
        if (validUntil == null) {
            logger.warn("validUntil is null — cannot calculate proration, returning ZERO");
            return new ProrationResult(BigDecimal.ZERO, 0L, 30L);
        }

        Instant now = Instant.now();


        long daysRemaining = ChronoUnit.DAYS.between(now, validUntil);
        if (daysRemaining < 0) daysRemaining = 0;

        long daysInCycle = resolveDaysInCycle(billingCycle, validUntil, currentPeriodStart);

        BigDecimal priceDifference = newPlanPriceArs.subtract(currentPlanPriceArs);

        if (priceDifference.compareTo(BigDecimal.ZERO) <= 0) {

            return new ProrationResult(BigDecimal.ZERO, daysRemaining, daysInCycle);
        }

        if (daysRemaining == 0) {
            return new ProrationResult(BigDecimal.ZERO, 0L, daysInCycle);
        }


        BigDecimal differential = priceDifference
                .multiply(BigDecimal.valueOf(daysRemaining))
                .divide(BigDecimal.valueOf(daysInCycle), 2, RoundingMode.HALF_UP);


        if (differential.compareTo(MIN_CHARGE) < 0) {
            differential = BigDecimal.ZERO;
        }

        logger.debug("Proration: priceDiff={} ARS, daysRemaining={}, daysInCycle={}, differential={} ARS",
                priceDifference, daysRemaining, daysInCycle, differential);

        return new ProrationResult(differential, daysRemaining, daysInCycle);
    }



    private long resolveDaysInCycle(String billingCycle, Instant validUntil, Instant currentPeriodStart) {
        if ("annual".equalsIgnoreCase(billingCycle)) {
            if (currentPeriodStart != null) {

                long days = ChronoUnit.DAYS.between(
                        currentPeriodStart.atZone(AR_ZONE).toLocalDate(),
                        validUntil.atZone(AR_ZONE).toLocalDate());
                if (days > 0) return days;
            }

            LocalDate periodEndDate = validUntil.atZone(AR_ZONE).toLocalDate();
            return periodEndDate.isLeapYear() ? 366L : 365L;
        }


        LocalDate periodEndDate = validUntil.atZone(AR_ZONE).toLocalDate();
        return periodEndDate.lengthOfMonth();
    }
}
