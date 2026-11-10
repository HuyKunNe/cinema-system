package com.cinema.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.inventory.dto.response.FareMatrixResponse;
import com.cinema.inventory.dto.response.FareMatrixResponse.CellResponse;
import com.cinema.inventory.dto.response.FareMatrixResponse.ScopeResponse;
import com.cinema.inventory.dto.response.FareMatrixResponse.TicketGroupResponse;
import com.cinema.inventory.dto.response.FareMatrixResponse.TimeBandResponse;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

class PricingFareResolverTest {

    private static final UUID REGULAR = UUID.randomUUID();
    private static final UUID MEMBER = UUID.randomUUID();
    private static final UUID DAY = UUID.randomUUID();
    private static final UUID EVENING = UUID.randomUUID();
    private static final List<TimeBandResponse> BANDS = List.of(
            new TimeBandResponse(DAY, "Day", 0, 1020, 0),
            new TimeBandResponse(EVENING, "Evening", 1020, 1440, 1));

    @Test
    void zeroIsAConfiguredFareForBothSeatAndPair() {
        for (SeatType type : SeatType.values()) {
            FareUnit unit = type == SeatType.COUPLE ? FareUnit.PAIR : FareUnit.SEAT;
            var cell = cell(ContentFormat.TWO_D, DAY, REGULAR, type,
                    FareCellState.PRICED, BigDecimal.ZERO, unit);

            assertSame(cell, resolve(matrix(List.of(cell), BANDS, true),
                    ContentFormat.TWO_D, LocalTime.NOON, null, type));
        }
    }

    @ParameterizedTest
    @MethodSource("invalidPricedCells")
    void rejectsMalformedPricedCellsAtTheSharedResolver(
            SeatType type, BigDecimal amount, FareUnit unit) {
        var cell = cell(ContentFormat.TWO_D, DAY, REGULAR, type,
                FareCellState.PRICED, amount, unit);

        var error = assertThrows(ConflictException.class,
                () -> resolve(matrix(List.of(cell), BANDS, true),
                        ContentFormat.TWO_D, LocalTime.NOON, REGULAR, type));

        assertEquals(InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT, error.getErrorCode());
    }

    @Test
    void pairPricesRemainIndependentAcrossFormatsGroupsAndTimeBands() {
        var cells = List.of(
                couple(ContentFormat.TWO_D, DAY, REGULAR, "180000"),
                couple(ContentFormat.TWO_D, DAY, MEMBER, "160000"),
                couple(ContentFormat.TWO_D, EVENING, REGULAR, "190000"),
                couple(ContentFormat.TWO_D, EVENING, MEMBER, "170000"),
                couple(ContentFormat.THREE_D, DAY, REGULAR, "230000"),
                couple(ContentFormat.THREE_D, DAY, MEMBER, "210000"),
                couple(ContentFormat.THREE_D, EVENING, REGULAR, "250000"),
                couple(ContentFormat.THREE_D, EVENING, MEMBER, "220000"));
        var matrix = matrix(cells, BANDS, true);

        assertSame(cells.get(0), resolve(matrix, ContentFormat.TWO_D, LocalTime.NOON, null, SeatType.COUPLE));
        assertSame(cells.get(1), resolve(matrix, ContentFormat.TWO_D, LocalTime.NOON, MEMBER, SeatType.COUPLE));
        assertSame(cells.get(2), resolve(matrix, ContentFormat.TWO_D, LocalTime.of(17, 0), REGULAR, SeatType.COUPLE));
        assertSame(cells.get(7), resolve(matrix, ContentFormat.THREE_D, LocalTime.of(23, 59), MEMBER, SeatType.COUPLE));
        assertEquals(new BigDecimal("180000"), cells.get(0).amount());
    }

    @Test
    void unavailableAndUnconfiguredAreNotTreatedAsFree() {
        for (FareCellState state : List.of(FareCellState.NOT_APPLICABLE, FareCellState.UNCONFIGURED)) {
            var cell = cell(ContentFormat.TWO_D, DAY, REGULAR, SeatType.STANDARD,
                    state, null, FareUnit.SEAT);

            var error = assertThrows(ConflictException.class,
                    () -> resolve(matrix(List.of(cell), BANDS, true),
                            ContentFormat.TWO_D, LocalTime.NOON, null, SeatType.STANDARD));

            assertEquals(state == FareCellState.UNCONFIGURED
                            ? InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT
                            : InventoryErrorCode.PRICING_FARE_NOT_APPLICABLE,
                    error.getErrorCode());
        }
    }

    @Test
    void incompleteCoverageIsRejectedEvenWhenTheRequestedCellIsPriced() {
        var cell = couple(ContentFormat.TWO_D, DAY, REGULAR, "180000");

        var error = assertThrows(ConflictException.class,
                () -> resolve(matrix(List.of(cell), BANDS, false),
                        ContentFormat.TWO_D, LocalTime.NOON, null, SeatType.COUPLE));

        assertEquals(InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT, error.getErrorCode());
    }

    @Test
    void overnightBandUsesExclusiveEndBoundary() {
        var bands = List.of(new TimeBandResponse(DAY, "Day", 120, 1320, 0),
                new TimeBandResponse(EVENING, "Night", 1320, 120, 1));
        var night = couple(ContentFormat.TWO_D, EVENING, REGULAR, "190000");
        var day = couple(ContentFormat.TWO_D, DAY, REGULAR, "180000");
        var matrix = matrix(List.of(day, night), bands, true);

        assertSame(night, resolve(matrix, ContentFormat.TWO_D, LocalTime.MIDNIGHT, null, SeatType.COUPLE));
        assertSame(night, resolve(matrix, ContentFormat.TWO_D, LocalTime.of(22, 0), null, SeatType.COUPLE));
        assertSame(day, resolve(matrix, ContentFormat.TWO_D, LocalTime.of(2, 0), null, SeatType.COUPLE));
    }

    private static Stream<Arguments> invalidPricedCells() {
        return Stream.of(
                Arguments.of(SeatType.STANDARD, null, FareUnit.SEAT),
                Arguments.of(SeatType.STANDARD, new BigDecimal("-1"), FareUnit.SEAT),
                Arguments.of(SeatType.STANDARD, BigDecimal.ONE, FareUnit.PAIR),
                Arguments.of(SeatType.COUPLE, BigDecimal.ONE, FareUnit.SEAT),
                Arguments.of(SeatType.COUPLE, BigDecimal.ONE, null));
    }

    private static CellResponse couple(ContentFormat format, UUID band, UUID group, String price) {
        return cell(format, band, group, SeatType.COUPLE,
                FareCellState.PRICED, new BigDecimal(price), FareUnit.PAIR);
    }

    private static CellResponse cell(ContentFormat format, UUID band, UUID group, SeatType type,
            FareCellState state, BigDecimal amount, FareUnit unit) {
        return new CellResponse(PricingExperienceType.STANDARD, format, band, group, type, state, amount, unit);
    }

    private static CellResponse resolve(FareMatrixResponse matrix, ContentFormat format,
            LocalTime time, UUID group, SeatType type) {
        return PricingFareResolver.resolve(matrix, time, PricingExperienceType.STANDARD, format, group, type);
    }

    private static FareMatrixResponse matrix(List<CellResponse> cells, List<TimeBandResponse> bands, boolean complete) {
        return new FareMatrixResponse(UUID.randomUUID(), UUID.randomUUID(), 1, 0L,
                PricingDayGroup.WEEKDAY, PricingPolicyVersionStatus.PUBLISHED,
                List.of(new ScopeResponse(PricingExperienceType.STANDARD, ContentFormat.TWO_D),
                        new ScopeResponse(PricingExperienceType.STANDARD, ContentFormat.THREE_D)),
                bands,
                List.of(new TicketGroupResponse(REGULAR, "Regular", null, true, 0),
                        new TicketGroupResponse(MEMBER, "Member", "Membership", false, 1)),
                REGULAR, cells.stream().map(CellResponse::seatType).distinct().toList(),
                cells.size(), cells.size(), complete ? 0 : 1, complete, cells, null);
    }
}
