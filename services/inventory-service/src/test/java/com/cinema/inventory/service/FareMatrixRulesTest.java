package com.cinema.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.FareMatrixCell.Key;
import com.cinema.inventory.entity.FareMatrixCell.Value;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.PricingTimeBand;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.TicketCategory;
import com.cinema.inventory.exception.InventoryErrorCode;

class FareMatrixRulesTest {

    @Test
    void holidayShouldRejectDifferentCategoryFaresIncludingZero() {
        for (String childAmount : List.of("240000", "0")) {
            Value adult =
                    priced(
                            ContentFormat.TWO_D,
                            PricingTimeBand.BEFORE_17,
                            TicketCategory.ADULT,
                            "260000");

            Value child =
                    priced(
                            ContentFormat.TWO_D,
                            PricingTimeBand.BEFORE_17,
                            TicketCategory.CHILD,
                            childAmount);

            ValidationException exception =
                    assertThrows(
                            ValidationException.class,
                            () ->
                                    FareMatrixRules.validateUniformFares(
                                            PricingDayGroup.HOLIDAY,
                                            Map.of(
                                                    adult.key(), adult,
                                                    child.key(), child)));

            assertThat(exception.getErrorCode())
                    .isSameAs(InventoryErrorCode.COUPLE_CATEGORY_FARE_CONFLICT);
        }
    }

    @Test
    void holidayShouldAllowNotApplicableCategory() {
        Value adult =
                priced(
                        ContentFormat.TWO_D,
                        PricingTimeBand.BEFORE_17,
                        TicketCategory.ADULT,
                        "260000");

        Value child =
                new Value(
                        key(ContentFormat.TWO_D, PricingTimeBand.BEFORE_17, TicketCategory.CHILD),
                        FareCellState.NOT_APPLICABLE,
                        null);

        assertDoesNotThrow(
                () ->
                        FareMatrixRules.validateUniformFares(
                                PricingDayGroup.HOLIDAY,
                                Map.of(
                                        adult.key(), adult,
                                        child.key(), child)));
    }

    @Test
    void holidayShouldKeepFormatsAndTimeBandsIndependent() {
        Value morning2d =
                priced(
                        ContentFormat.TWO_D,
                        PricingTimeBand.BEFORE_17,
                        TicketCategory.ADULT,
                        "260000");

        Value evening2d =
                priced(
                        ContentFormat.TWO_D,
                        PricingTimeBand.FROM_17,
                        TicketCategory.ADULT,
                        "280000");

        Value morning3d =
                priced(
                        ContentFormat.THREE_D,
                        PricingTimeBand.BEFORE_17,
                        TicketCategory.ADULT,
                        "310000");

        Value evening3d =
                priced(
                        ContentFormat.THREE_D,
                        PricingTimeBand.FROM_17,
                        TicketCategory.ADULT,
                        "330000");

        assertDoesNotThrow(
                () ->
                        FareMatrixRules.validateUniformFares(
                                PricingDayGroup.HOLIDAY,
                                Map.of(
                                        morning2d.key(), morning2d,
                                        evening2d.key(), evening2d,
                                        morning3d.key(), morning3d,
                                        evening3d.key(), evening3d)));
    }

    @Test
    void nonHolidayShouldStillRejectDifferentTimeBandFares() {
        Value morning =
                priced(
                        ContentFormat.TWO_D,
                        PricingTimeBand.BEFORE_17,
                        TicketCategory.ADULT,
                        "200000");

        Value evening =
                priced(
                        ContentFormat.TWO_D,
                        PricingTimeBand.FROM_17,
                        TicketCategory.ADULT,
                        "210000");

        ValidationException exception =
                assertThrows(
                        ValidationException.class,
                        () ->
                                FareMatrixRules.validateUniformFares(
                                        PricingDayGroup.WEEKDAY,
                                        Map.of(
                                                morning.key(), morning,
                                                evening.key(), evening)));

        assertThat(exception.getErrorCode())
                .isSameAs(InventoryErrorCode.COUPLE_FIXED_FARE_CONFLICT);
    }

    private Value priced(
            ContentFormat format,
            PricingTimeBand timeBand,
            TicketCategory category,
            String amount) {

        return new Value(
                key(format, timeBand, category), FareCellState.PRICED, new BigDecimal(amount));
    }

    private Key key(ContentFormat format, PricingTimeBand timeBand, TicketCategory category) {

        UUID timeBandId =
                UUID.fromString(
                        timeBand == PricingTimeBand.BEFORE_17
                                ? "019a0c12-1000-7000-8000-000000000001"
                                : "019a0c12-1000-7000-8000-000000000002");

        UUID ticketGroupId =
                UUID.fromString(
                        "019a0c12-1000-7000-8000-%012d".formatted(100 + category.ordinal()));

        return new Key(
                PricingExperienceType.STANDARD, format, timeBandId, ticketGroupId, SeatType.COUPLE);
    }
}
