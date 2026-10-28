package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.inventory.dto.response.FareMatrixResponse.CellResponse;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.event.payload.RequestedSeatPayload;
import com.cinema.inventory.event.payload.ReservedSeatPayload;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.ShowtimeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class SeatReservationPricingService {

    private final ShowtimeRepository showtimeRepository;
    private final FareMatrixService fareMatrixService;

    public SeatReservationPricingService(
            ShowtimeRepository showtimeRepository, FareMatrixService fareMatrixService) {

        this.showtimeRepository = showtimeRepository;
        this.fareMatrixService = fareMatrixService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Resolution resolve(
            UUID showtimeId,
            List<ShowSeat> lockedSeats,
            List<RequestedSeatPayload> requestedSeats) {

        var showtime = showtimeRepository.findByIdForSeatMap(showtimeId).orElse(null);

        if (showtime == null || lockedSeats.isEmpty()) {
            return Resolution.unavailable();
        }

        var selected = showtime.getPricingPolicyVersion();

        if (selected == null || selected.getStatus() != PricingPolicyVersionStatus.PUBLISHED) {

            return Resolution.unavailable();
        }

        var room = showtime.getRoom();
        var cinema = room.getCinema();
        var screening = showtime.getScreeningVersion();

        if (!selected.appliesTo(cinema.getId())
                || room.getRoomType() == null
                || screening == null
                || screening.getContentFormat() == null) {

            return Resolution.unavailable();
        }

        Map<String, UUID> groupsBySeat = new HashMap<>();

        for (RequestedSeatPayload requested : requestedSeats) {
            String number = normalize(requested.seatNumber());

            if (groupsBySeat.containsKey(number)) {
                throw inconsistent();
            }

            groupsBySeat.put(number, requested.ticketGroupId());
        }

        if (groupsBySeat.size() != lockedSeats.size()) {
            throw inconsistent();
        }

        var matrix =
                fareMatrixService.getForShowtimePricing(
                        cinema.getId(), selected.getPolicy().getId(), selected.getId());

        var localTime =
                showtime.getStartsAt()
                        .atZoneSameInstant(PricingFareResolver.cinemaZone(cinema.getTimeZone()))
                        .toLocalTime();

        var experience = room.getRoomType().getPricingExperienceType();
        var format = screening.getContentFormat();

        Map<FareKey, CellResponse> fares = new HashMap<>();
        List<ReservedSeatPayload> snapshots = new ArrayList<>();

        for (ShowSeat seat : lockedSeats) {
            String number = normalize(seat.getSeatNumber());

            if (!groupsBySeat.containsKey(number)
                    || !showtimeId.equals(seat.getShowtime().getId())) {

                throw inconsistent();
            }

            UUID requestedGroupId = groupsBySeat.get(number);

            UUID groupId =
                    requestedGroupId == null ? matrix.defaultTicketGroupId() : requestedGroupId;

            FareKey key = new FareKey(seat.getSeatType(), groupId);

            CellResponse fare;

            try {
                fare =
                        fares.computeIfAbsent(
                                key,
                                ignored ->
                                        PricingFareResolver.resolve(
                                                matrix,
                                                localTime,
                                                experience,
                                                format,
                                                groupId,
                                                seat.getSeatType()));

            } catch (ConflictException exception) {
                if (exception.getErrorCode() == InventoryErrorCode.PRICING_FARE_NOT_APPLICABLE) {

                    return Resolution.unavailable();
                }

                throw exception;
            }

            FareUnit expectedUnit =
                    seat.getSeatType() == SeatType.COUPLE ? FareUnit.PAIR : FareUnit.SEAT;

            if (fare.amount() == null
                    || fare.amount().signum() < 0
                    || fare.unit() != expectedUnit) {

                throw inconsistent();
            }

            var group =
                    matrix.ticketGroups().stream()
                            .filter(candidate -> candidate.id().equals(fare.ticketGroupId()))
                            .findFirst()
                            .orElseThrow(SeatReservationPricingService::inconsistent);

            snapshots.add(
                    new ReservedSeatPayload(
                            seat.getId(),
                            seat.getSeatNumber(),
                            seat.getSeatType(),
                            fare.amount(),
                            fare.ticketGroupId(),
                            group.name()));
        }

        return new Resolution(true, snapshots);
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static ConflictException inconsistent() {
        return new ConflictException(InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT);
    }

    private record FareKey(SeatType seatType, UUID ticketGroupId) {}

    public record Resolution(boolean applicable, List<ReservedSeatPayload> seats) {

        public Resolution {
            seats = List.copyOf(seats);
        }

        private static Resolution unavailable() {
            return new Resolution(false, List.of());
        }
    }
}
