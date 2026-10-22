package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.inventory.client.MovieScreeningClient;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.ShowSeatRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class PolicyShowtimeOpeningService {

    private final MovieScreeningClient movieClient;
    private final ShowtimeLayoutService layoutService;
    private final ShowtimePreparationValidator preparationValidator;
    private final ShowSeatRepository showSeatRepository;
    private final Clock clock;
    private final boolean enabled;

    public PolicyShowtimeOpeningService(
            MovieScreeningClient movieClient,
            ShowtimeLayoutService layoutService,
            ShowtimePreparationValidator preparationValidator,
            ShowSeatRepository showSeatRepository,
            Clock clock,
            @Value("${cinema.inventory.policy-booking.enabled:false}") boolean enabled) {

        this.movieClient = movieClient;
        this.layoutService = layoutService;
        this.preparationValidator = preparationValidator;
        this.showSeatRepository = showSeatRepository;
        this.clock = clock;
        this.enabled = enabled;
    }

    /** Caller already holds cinema, room and showtime mutation locks. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void open(Showtime showtime) {
        if (!enabled) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_POLICY_BOOKING_NOT_READY);
        }

        if (!showtime.usesPolicyPricing()
                || showtime.getExperienceType() == null
                || showtime.getContentFormat() == null
                || showtime.getOfficialReleaseDate() == null
                || showtime.getMovieVersion() == null
                || showtime.getMovieVersion() < 0
                || showtime.getRoomLayout() == null
                || !Objects.equals(
                        showtime.getRoomLayoutVersion(),
                        Long.valueOf(showtime.getRoomLayout().getLayoutVersion()))
                || !Objects.equals(
                        showtime.getPricingPolicyVersion(),
                        Long.valueOf(showtime.getPricingPolicy().getPolicyVersion()))
                || !showtime.getPricingPolicy()
                        .getCinema()
                        .getId()
                        .equals(showtime.getRoom().getCinema().getId())) {
            throw invalidBinding();
        }

        var movie = movieClient.get(showtime.getMovieId());

        preparationValidator.requireScreening(
                showtime.getRoom(),
                movie,
                showtime.getExperienceType(),
                showtime.getContentFormat());

        if (!movie.releaseDate().equals(showtime.getOfficialReleaseDate())) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_MOVIE_RELEASE_CHANGED);
        }

        // Use the pinned layout, not the newest published layout.
        var snapshots = layoutService.getSeatSnapshotsForGeneration(showtime);

        preparationValidator.requirePolicyCoverage(
                showtime.getPricingPolicy(),
                showtime.getExperienceType(),
                showtime.getContentFormat(),
                showtime.getOfficialReleaseDate(),
                showtime.getStartsAt(),
                snapshots);

        Map<UUID, LayoutSeatSnapshot> expected = new HashMap<>();
        Set<String> expectedNumbers = new HashSet<>();

        for (var snapshot : snapshots) {
            if (expected.putIfAbsent(snapshot.seat().getId(), snapshot) != null
                    || !expectedNumbers.add(snapshot.seatNumber())) {
                throw inconsistentSeats();
            }
        }

        var seats = showSeatRepository.findAllForOpening(showtime.getId());

        if (expected.isEmpty() || seats.size() != expected.size()) {
            throw inconsistentSeats();
        }

        Set<UUID> foundSeatIds = new HashSet<>();
        boolean hasAvailableSeat = false;

        for (var seat : seats) {
            UUID physicalSeatId = seat.getSeat().getId();
            var snapshot = expected.get(physicalSeatId);

            if (!foundSeatIds.add(physicalSeatId)
                    || snapshot == null
                    || !Objects.equals(snapshot.seatNumber(), seat.getSeatNumber())
                    || snapshot.seatType() != seat.getSeatType()
                    || seat.getHeldByBookingId() != null
                    || seat.getHoldExpiresAt() != null
                    || (seat.getStatus() != ShowSeatStatus.AVAILABLE
                            && seat.getStatus() != ShowSeatStatus.UNAVAILABLE)) {
                throw inconsistentSeats();
            }

            if (seat.getPrice() != null) {
                throw new ConflictException(InventoryErrorCode.SHOWTIME_POLICY_PRICE_NOT_ALLOWED);
            }

            if (seat.getStatus() == ShowSeatStatus.AVAILABLE) {
                if (!seat.getSeat().isActive()) {
                    throw inconsistentSeats();
                }

                hasAvailableSeat = true;
            }
        }

        if (!hasAvailableSeat) {
            throw new ConflictException(InventoryErrorCode.NO_ACTIVE_SEATS);
        }

        // Recheck time after database locks and remote capability lookup.
        showtime.openPolicyForBooking(OffsetDateTime.now(clock));
    }

    private static ConflictException invalidBinding() {
        return new ConflictException(InventoryErrorCode.SHOWTIME_POLICY_BINDING_INVALID);
    }

    private static ConflictException inconsistentSeats() {
        return new ConflictException(InventoryErrorCode.SEAT_MAP_DATA_INCONSISTENT);
    }
}
