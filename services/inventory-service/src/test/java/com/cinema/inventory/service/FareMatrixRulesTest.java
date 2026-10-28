package com.cinema.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.FareMatrixCell.Key;
import com.cinema.inventory.entity.FareMatrixCell.Value;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class FareMatrixRulesTest {

    private static final UUID MORNING = UUID.randomUUID();
    private static final UUID EVENING = UUID.randomUUID();
    private static final UUID REGULAR = UUID.randomUUID();
    private static final UUID MEMBER = UUID.randomUUID();

    @Test
    void coupleCellsShouldAllowIndependentPricesWithoutChangingValues() {
        for (var experience : PricingExperienceType.values()) {
            var values =
                    fares(
                            couple(experience, ContentFormat.TWO_D, MORNING, REGULAR, "180000"),
                            couple(experience, ContentFormat.TWO_D, EVENING, REGULAR, "190000"),
                            couple(experience, ContentFormat.TWO_D, MORNING, MEMBER, "160000"),
                            couple(experience, ContentFormat.TWO_D, EVENING, MEMBER, "170000"),
                            couple(experience, ContentFormat.THREE_D, MORNING, REGULAR, "230000"),
                            couple(experience, ContentFormat.THREE_D, EVENING, REGULAR, "250000"),
                            couple(experience, ContentFormat.THREE_D, MORNING, MEMBER, "210000"),
                            couple(experience, ContentFormat.THREE_D, EVENING, MEMBER, "0"));

            Map<Key, Value> original = Map.copyOf(values);

            assertDoesNotThrow(() -> FareMatrixRules.validateUniformFares(values));

            assertThat(values).isEqualTo(original);
        }
    }

    @Test
    void coupleShouldAllowNotApplicableCells() {
        Value morning =
                couple(
                        PricingExperienceType.PREMIUM_CINEMA,
                        ContentFormat.TWO_D,
                        MORNING,
                        MEMBER,
                        "180000");

        Value evening =
                new Value(
                        new Key(
                                PricingExperienceType.PREMIUM_CINEMA,
                                ContentFormat.TWO_D,
                                EVENING,
                                MEMBER,
                                SeatType.COUPLE),
                        FareCellState.NOT_APPLICABLE,
                        null);

        assertDoesNotThrow(() -> FareMatrixRules.validateUniformFares(fares(morning, evening)));
    }

    @Test
    void nonCouplePremiumFaresShouldKeepExistingGroupRule() {
        Value regular =
                new Value(
                        new Key(
                                PricingExperienceType.PREMIUM_CINEMA,
                                ContentFormat.TWO_D,
                                MORNING,
                                REGULAR,
                                SeatType.STANDARD),
                        FareCellState.PRICED,
                        new BigDecimal("120000"));

        Value member =
                new Value(
                        new Key(
                                PricingExperienceType.PREMIUM_CINEMA,
                                ContentFormat.TWO_D,
                                MORNING,
                                MEMBER,
                                SeatType.STANDARD),
                        FareCellState.PRICED,
                        new BigDecimal("100000"));

        ValidationException exception =
                assertThrows(
                        ValidationException.class,
                        () -> FareMatrixRules.validateUniformFares(fares(regular, member)));

        assertThat(exception.getErrorCode())
                .isSameAs(InventoryErrorCode.PREMIUM_CATEGORY_FARE_CONFLICT);
    }

    private static Value couple(
            PricingExperienceType experience,
            ContentFormat format,
            UUID timeBandId,
            UUID ticketGroupId,
            String amount) {

        return new Value(
                new Key(experience, format, timeBandId, ticketGroupId, SeatType.COUPLE),
                FareCellState.PRICED,
                new BigDecimal(amount));
    }

    private static Map<Key, Value> fares(Value... values) {
        Map<Key, Value> result = new HashMap<>();

        for (Value value : values) {
            result.put(value.key(), value);
        }

        return result;
    }
}
