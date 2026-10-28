package com.cinema.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.FareMatrixCell.Key;
import com.cinema.inventory.entity.FareMatrixCell.Value;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class FareMatrixRulesTest {

    private static final UUID MORNING = UUID.randomUUID();
    private static final UUID EVENING = UUID.randomUUID();
    private static final UUID REGULAR = UUID.randomUUID();
    private static final UUID MEMBER = UUID.randomUUID();

    @Test
    void nonHolidayShouldAllowDifferentPricesBetweenCustomerGroups() {
        for (var day : nonHolidayDays()) {
            for (var experience : PricingExperienceType.values()) {
                var values =
                        fares(
                                priced(experience, ContentFormat.TWO_D, MORNING, REGULAR, "180000"),
                                priced(experience, ContentFormat.TWO_D, EVENING, REGULAR, "180000"),
                                priced(experience, ContentFormat.TWO_D, MORNING, MEMBER, "160000"),
                                priced(experience, ContentFormat.TWO_D, EVENING, MEMBER, "160000"));

                assertDoesNotThrow(() -> FareMatrixRules.validateUniformFares(day, values));
            }
        }
    }

    @Test
    void nonHolidayShouldRejectDifferentTimeBandPricesWithinSameGroup() {
        for (var day : nonHolidayDays()) {
            for (var experience : PricingExperienceType.values()) {
                for (String eveningPrice : List.of("190000", "0")) {
                    var values =
                            fares(
                                    priced(
                                            experience,
                                            ContentFormat.TWO_D,
                                            MORNING,
                                            MEMBER,
                                            "160000"),
                                    priced(
                                            experience,
                                            ContentFormat.TWO_D,
                                            EVENING,
                                            MEMBER,
                                            eveningPrice));

                    ValidationException exception =
                            assertThrows(
                                    ValidationException.class,
                                    () -> FareMatrixRules.validateUniformFares(day, values));

                    assertThat(exception.getErrorCode())
                            .isSameAs(InventoryErrorCode.COUPLE_FIXED_FARE_CONFLICT);
                }
            }
        }
    }

    @Test
    void holidayShouldAllowDifferentGroupsAndTimeBandPrices() {
        for (var experience : PricingExperienceType.values()) {
            var values =
                    fares(
                            priced(experience, ContentFormat.TWO_D, MORNING, REGULAR, "260000"),
                            priced(experience, ContentFormat.TWO_D, EVENING, REGULAR, "280000"),
                            priced(experience, ContentFormat.TWO_D, MORNING, MEMBER, "220000"),
                            priced(experience, ContentFormat.TWO_D, EVENING, MEMBER, "240000"));

            assertDoesNotThrow(
                    () -> FareMatrixRules.validateUniformFares(PricingDayGroup.HOLIDAY, values));
        }
    }

    @Test
    void formatsShouldHaveIndependentPrices() {
        for (var day : nonHolidayDays()) {
            for (var experience : PricingExperienceType.values()) {
                var values =
                        fares(
                                priced(experience, ContentFormat.TWO_D, MORNING, MEMBER, "160000"),
                                priced(experience, ContentFormat.TWO_D, EVENING, MEMBER, "160000"),
                                priced(
                                        experience,
                                        ContentFormat.THREE_D,
                                        MORNING,
                                        MEMBER,
                                        "210000"),
                                priced(
                                        experience,
                                        ContentFormat.THREE_D,
                                        EVENING,
                                        MEMBER,
                                        "210000"));

                assertDoesNotThrow(() -> FareMatrixRules.validateUniformFares(day, values));
            }
        }
    }

    private static List<PricingDayGroup> nonHolidayDays() {
        return List.of(PricingDayGroup.WEEKDAY, PricingDayGroup.WEEKEND);
    }

    private static Value priced(
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
