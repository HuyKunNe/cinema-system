package com.cinema.inventory.service;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.QuotePricingFareRequest;
import com.cinema.inventory.dto.response.PricingFareQuoteResponse;
import com.cinema.inventory.entity.Cinema;
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

        ZoneId cinemaZone = PricingFareResolver.cinemaZone(cinema.getTimeZone());

        var localStartsAt = request.startsAt().atZoneSameInstant(cinemaZone).toOffsetDateTime();

        var fare =
                PricingFareResolver.resolve(
                        matrix,
                        localStartsAt.toLocalTime(),
                        request.experienceType(),
                        request.contentFormat(),
                        request.ticketGroupId(),
                        request.seatType());

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
                || request.seatType() == null) {

            throw new ValidationException(InventoryErrorCode.INVALID_PRICING_FARE_QUOTE);
        }
    }
}
