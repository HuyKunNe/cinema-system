package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.QuotePricingFareRequest;
import com.cinema.inventory.dto.response.PricingFareQuoteResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.enums.PricingTimeBand;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.CinemaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.UUID;

@Service
public class PricingFareQuoteService {

    private final CinemaRepository cinemaRepository;
    private final FareMatrixService fareMatrixService;

    public PricingFareQuoteService(
            CinemaRepository cinemaRepository, FareMatrixService fareMatrixService) {

        this.cinemaRepository = cinemaRepository;
        this.fareMatrixService = fareMatrixService;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PricingFareQuoteResponse quote(
            UUID cinemaId, UUID policyId, UUID versionId, QuotePricingFareRequest request) {

        validateRequest(request);

        Cinema cinema =
                cinemaRepository
                        .findById(cinemaId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND));

        var matrix = fareMatrixService.get(cinemaId, policyId, versionId);

        if (matrix.status() != PricingPolicyVersionStatus.PUBLISHED) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_NOT_PUBLISHED);
        }

        if (!matrix.coverageComplete()) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT);
        }

        ZoneId cinemaZone = resolveCinemaZone(cinema);

        var localStartsAt = request.startsAt().atZoneSameInstant(cinemaZone).toOffsetDateTime();

        PricingTimeBand timeBand = PricingTimeBand.fromLocalTime(localStartsAt.toLocalTime());

        var fare =
                matrix.cells().stream()
                        .filter(
                                cell ->
                                        cell.experienceType() == request.experienceType()
                                                && cell.contentFormat() == request.contentFormat()
                                                && cell.timeBand() == timeBand
                                                && cell.ticketCategory() == request.ticketCategory()
                                                && cell.seatType() == request.seatType())
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new ConflictException(
                                                InventoryErrorCode.PRICING_FARE_NOT_APPLICABLE));

        if (fare.state() == FareCellState.UNCONFIGURED) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT);
        }

        if (fare.state() != FareCellState.PRICED) {
            throw new ConflictException(InventoryErrorCode.PRICING_FARE_NOT_APPLICABLE);
        }

        return new PricingFareQuoteResponse(
                matrix.policyId(),
                matrix.policyVersionId(),
                matrix.revision(),
                cinemaId,
                cinemaZone.getId(),
                localStartsAt,
                matrix.dayGroup(),
                request.seatType().getCapacity(),
                fare);
    }

    private static void validateRequest(QuotePricingFareRequest request) {

        if (request == null
                || request.startsAt() == null
                || request.experienceType() == null
                || request.contentFormat() == null
                || request.ticketCategory() == null
                || request.seatType() == null) {

            throw new ValidationException(InventoryErrorCode.INVALID_PRICING_FARE_QUOTE);
        }
    }

    private static ZoneId resolveCinemaZone(Cinema cinema) {
        String configuredTimeZone = cinema.getTimeZone();

        String zoneName = configuredTimeZone == null ? "Asia/Ho_Chi_Minh" : configuredTimeZone;

        if (!ZoneId.getAvailableZoneIds().contains(zoneName)) {
            throw new ValidationException(InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }

        return ZoneId.of(zoneName);
    }
}
