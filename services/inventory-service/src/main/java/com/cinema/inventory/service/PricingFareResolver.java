package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.response.FareMatrixResponse;
import com.cinema.inventory.dto.response.FareMatrixResponse.CellResponse;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

public final class PricingFareResolver {

    private PricingFareResolver() {}

    public static ZoneId cinemaZone(String configured) {
        String zoneName =
                configured == null
                        ? "Asia/Ho_Chi_Minh"
                        : configured;

        if (!ZoneId.getAvailableZoneIds().contains(zoneName)) {
            throw new ValidationException(
                    InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }

        return ZoneId.of(zoneName);
    }

    public static CellResponse resolve(
            FareMatrixResponse matrix,
            LocalTime localStartsAt,
            PricingExperienceType experienceType,
            ContentFormat contentFormat,
            UUID requestedTicketGroupId,
            SeatType seatType) {

        if (matrix.status() != PricingPolicyVersionStatus.PUBLISHED) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_POLICY_VERSION_NOT_PUBLISHED);
        }

        if (!matrix.coverageComplete()) {
            throw inconsistent();
        }

        UUID ticketGroupId =
                requestedTicketGroupId == null
                        ? matrix.defaultTicketGroupId()
                        : requestedTicketGroupId;

        if (ticketGroupId == null) {
            throw inconsistent();
        }

        boolean hasTicketGroup =
                matrix.ticketGroups().stream()
                        .anyMatch(group -> group.id().equals(ticketGroupId));

        if (!hasTicketGroup) {
            throw unavailable();
        }

        int minute =
                localStartsAt.getHour() * 60
                        + localStartsAt.getMinute();

        var matchingBands =
                matrix.timeBands().stream()
                        .filter(
                                band ->
                                        contains(
                                                minute,
                                                band.startMinute(),
                                                band.endMinute()))
                        .toList();

        if (matchingBands.isEmpty()) {
            throw unavailable();
        }

        if (matchingBands.size() != 1) {
            throw inconsistent();
        }

        UUID timeBandId = matchingBands.getFirst().id();

        CellResponse fare =
                matrix.cells().stream()
                        .filter(
                                cell ->
                                        cell.experienceType() == experienceType
                                                && cell.contentFormat() == contentFormat
                                                && cell.timeBandId().equals(timeBandId)
                                                && cell.ticketGroupId().equals(ticketGroupId)
                                                && cell.seatType() == seatType)
                        .findFirst()
                        .orElseThrow(PricingFareResolver::unavailable);

        if (fare.state() == FareCellState.UNCONFIGURED) {
            throw inconsistent();
        }

        if (fare.state() != FareCellState.PRICED) {
            throw unavailable();
        }

        return fare;
    }

    private static boolean contains(
            int minute, int startMinute, int endMinute) {

        if (endMinute > startMinute) {
            return minute >= startMinute && minute < endMinute;
        }

        return minute >= startMinute || minute < endMinute;
    }

    private static ConflictException unavailable() {
        return new ConflictException(
                InventoryErrorCode.PRICING_FARE_NOT_APPLICABLE);
    }

    private static ConflictException inconsistent() {
        return new ConflictException(
                InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT);
    }
}
