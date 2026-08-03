package com.cinema.inventory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;

@Component
public class SeatPricingPolicy {

    private static final BigDecimal STANDARD_MULTIPLIER = new BigDecimal("1.00");

    private static final BigDecimal VIP_MULTIPLIER = new BigDecimal("1.30");

    private static final BigDecimal COUPLE_MULTIPLIER = new BigDecimal("2.00");

    private static final BigDecimal ACCESSIBLE_MULTIPLIER = new BigDecimal("1.00");

    public BigDecimal calculate(
            BigDecimal basePrice,
            SeatType seatType) {

        validate(basePrice, seatType);

        return basePrice
                .multiply(multiplierOf(seatType))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal multiplierOf(SeatType seatType) {
        return switch (seatType) {
            case STANDARD -> STANDARD_MULTIPLIER;
            case VIP -> VIP_MULTIPLIER;
            case COUPLE -> COUPLE_MULTIPLIER;
            case ACCESSIBLE -> ACCESSIBLE_MULTIPLIER;
        };
    }

    private void validate(
            BigDecimal basePrice,
            SeatType seatType) {

        if (basePrice == null) {
            throw new ValidationException(
                    InventoryErrorCode.INV_BASE_PRICE_REQUIRED);
        }

        if (basePrice.signum() <= 0) {
            throw new ValidationException(
                    InventoryErrorCode.INV_INVALID_BASE_PRICE);
        }

        if (seatType == null) {
            throw new ValidationException(
                    InventoryErrorCode.INV_SEAT_TYPE_REQUIRED);
        }
    }
}
