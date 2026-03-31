package com.mipyme.company;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ProrationCalculatorServiceTest {

    private final ProrationCalculatorService calculator = new ProrationCalculatorService();



    @Test
    void monthlyUpgrade_halfMonthRemaining() {
        BigDecimal currentPrice = new BigDecimal("9000.00");
        BigDecimal newPrice = new BigDecimal("19000.00");
        Instant now = Instant.now();

        LocalDate endDate = now.atZone(ZoneId.of("America/Argentina/Buenos_Aires"))
                .toLocalDate().plusDays(15);
        Instant validUntil = endDate.atStartOfDay(ZoneId.of("America/Argentina/Buenos_Aires")).toInstant();
        Instant periodStart = validUntil.minus(30, ChronoUnit.DAYS);

        var result = calculator.calculate(currentPrice, newPrice, "monthly", validUntil, periodStart);

        assertThat(result.requiresPayment()).isTrue();

        assertThat(result.differential()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.daysRemaining()).isGreaterThanOrEqualTo(14);
    }

    @Test
    void monthlyUpgrade_oneDayRemaining() {
        BigDecimal currentPrice = new BigDecimal("9000.00");
        BigDecimal newPrice = new BigDecimal("19000.00");
        Instant validUntil = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant periodStart = validUntil.minus(30, ChronoUnit.DAYS);

        var result = calculator.calculate(currentPrice, newPrice, "monthly", validUntil, periodStart);

        assertThat(result.requiresPayment()).isTrue();
        assertThat(result.daysRemaining()).isEqualTo(1);

        assertThat(result.differential()).isLessThan(new BigDecimal("500.00"));
    }

    @Test
    void monthlyUpgrade_zeroDaysRemaining() {
        BigDecimal currentPrice = new BigDecimal("9000.00");
        BigDecimal newPrice = new BigDecimal("19000.00");
        Instant validUntil = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant periodStart = validUntil.minus(30, ChronoUnit.DAYS);

        var result = calculator.calculate(currentPrice, newPrice, "monthly", validUntil, periodStart);

        assertThat(result.requiresPayment()).isFalse();
        assertThat(result.differential()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void monthlyDowngrade_neverCharges() {
        BigDecimal currentPrice = new BigDecimal("19000.00");
        BigDecimal newPrice = new BigDecimal("9000.00");
        Instant validUntil = Instant.now().plus(15, ChronoUnit.DAYS);
        Instant periodStart = validUntil.minus(30, ChronoUnit.DAYS);

        var result = calculator.calculate(currentPrice, newPrice, "monthly", validUntil, periodStart);

        assertThat(result.requiresPayment()).isFalse();
    }



    @Test
    void annualUpgrade_halfYearRemaining() {
        BigDecimal currentPrice = new BigDecimal("90000.00");
        BigDecimal newPrice = new BigDecimal("190000.00");
        Instant periodStart = Instant.now().minus(182, ChronoUnit.DAYS);
        Instant validUntil = periodStart.plus(365, ChronoUnit.DAYS);

        var result = calculator.calculate(currentPrice, newPrice, "annual", validUntil, periodStart);

        assertThat(result.requiresPayment()).isTrue();

        assertThat(result.differential()).isGreaterThan(new BigDecimal("45000.00"));
        assertThat(result.differential()).isLessThan(new BigDecimal("55000.00"));
    }

    @Test
    void annualUpgrade_handlesLeapYear() {
        BigDecimal currentPrice = new BigDecimal("90000.00");
        BigDecimal newPrice = new BigDecimal("190000.00");

        Instant periodStart = Instant.now().minus(100, ChronoUnit.DAYS);
        Instant validUntil = periodStart.plus(366, ChronoUnit.DAYS);

        var result = calculator.calculate(currentPrice, newPrice, "annual", validUntil, periodStart);

        assertThat(result.requiresPayment()).isTrue();
        assertThat(result.daysInCycle()).isGreaterThanOrEqualTo(365L);
    }



    @Test
    void nullValidUntil_returnsZero() {
        var result = calculator.calculate(
                new BigDecimal("9000"), new BigDecimal("19000"),
                "monthly", null, null);

        assertThat(result.requiresPayment()).isFalse();
        assertThat(result.differential()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void microPaymentSuppressed_returnsZero() {

        BigDecimal currentPrice = new BigDecimal("9000.00");
        BigDecimal newPrice = new BigDecimal("9010.00");
        Instant validUntil = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant periodStart = validUntil.minus(30, ChronoUnit.DAYS);

        var result = calculator.calculate(currentPrice, newPrice, "monthly", validUntil, periodStart);


        assertThat(result.requiresPayment()).isFalse();
    }

    @Test
    void samePriceReturnsZero() {
        BigDecimal price = new BigDecimal("19000.00");
        Instant validUntil = Instant.now().plus(15, ChronoUnit.DAYS);

        var result = calculator.calculate(price, price, "monthly", validUntil, null);

        assertThat(result.requiresPayment()).isFalse();
    }

    @Test
    void nullPrices_throwsException() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(null, new BigDecimal("100"), "monthly", Instant.now(), null));
    }
}
