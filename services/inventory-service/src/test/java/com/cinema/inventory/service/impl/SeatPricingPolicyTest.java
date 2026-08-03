package com.cinema.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.service.SeatPricingPolicy;

class SeatPricingPolicyTest {

    private SeatPricingPolicy seatPricingPolicy;

    @BeforeEach
    void setUp() {
        seatPricingPolicy = new SeatPricingPolicy();
    }

    @Test
    void calculateShouldReturnStandardPrice() {
        BigDecimal result = seatPricingPolicy.calculate(
                new BigDecimal("100000.00"),
                SeatType.STANDARD);

        assertThat(result)
                .isEqualByComparingTo("100000.00");
    }

    @Test
    void calculateShouldReturnVipPrice() {
        BigDecimal result = seatPricingPolicy.calculate(
                new BigDecimal("100000.00"),
                SeatType.VIP);

        assertThat(result)
                .isEqualByComparingTo("130000.00");
    }

    @Test
    void calculateShouldReturnCouplePriceForTwoPeople() {
        BigDecimal result = seatPricingPolicy.calculate(
                new BigDecimal("100000.00"),
                SeatType.COUPLE);

        assertThat(result)
                .isEqualByComparingTo("200000.00");

        assertThat(SeatType.COUPLE.getCapacity())
                .isEqualTo(2);
    }

    @Test
    void calculateShouldReturnAccessiblePrice() {
        BigDecimal result = seatPricingPolicy.calculate(
                new BigDecimal("100000.00"),
                SeatType.ACCESSIBLE);

        assertThat(result)
                .isEqualByComparingTo("100000.00");
    }

    @Test
    void calculateShouldRejectNullBasePrice() {
        assertThatThrownBy(() -> seatPricingPolicy.calculate(
                null,
                SeatType.STANDARD))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Base price is required");
    }

    @Test
    void calculateShouldRejectNonPositiveBasePrice() {
        assertThatThrownBy(() -> seatPricingPolicy.calculate(
                BigDecimal.ZERO,
                SeatType.STANDARD))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Base price must be greater than zero");
    }

    @Test
    void calculateShouldRejectNullSeatType() {
        assertThatThrownBy(() -> seatPricingPolicy.calculate(
                new BigDecimal("100000.00"),
                null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Seat type is required");
    }
}
