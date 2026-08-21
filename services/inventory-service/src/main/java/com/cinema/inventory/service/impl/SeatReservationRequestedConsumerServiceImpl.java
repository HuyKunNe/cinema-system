package com.cinema.inventory.service.impl;

import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.service.OutboxService;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.SeatReservationRejectedOutboxFactory;
import com.cinema.inventory.event.SeatReservationRejectionReason;
import com.cinema.inventory.event.SeatReservedOutboxFactory;
import com.cinema.inventory.event.payload.RequestedSeatPayload;
import com.cinema.inventory.event.payload.SeatReservationRequestedPayload;
import com.cinema.inventory.event.serialization.SeatReservationRequestedPayloadReader;
import com.cinema.inventory.event.validation.SeatReservationRequestedMessageValidator;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.ProcessedEventRegistrationService;
import com.cinema.inventory.service.SeatReservationRequestedConsumerService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SeatReservationRequestedConsumerServiceImpl
        implements SeatReservationRequestedConsumerService {

    private static final String SEAT_NOT_FOUND_MESSAGE =
            "One or more requested seats were not found";

    private static final String SEAT_UNAVAILABLE_MESSAGE =
            "One or more requested seats are unavailable";

    private static final String DUPLICATE_SEAT_MESSAGE =
            "Reservation request contains duplicate seat numbers";

    private static final String INVALID_REQUEST_MESSAGE = "Reservation request is invalid";

    private static final String RESERVATION_EXPIRED_MESSAGE = "Reservation request has expired";

    private final SeatReservationRequestedMessageValidator messageValidator;

    private final SeatReservationRequestedPayloadReader payloadReader;

    private final ProcessedEventRegistrationService processedEventRegistrationService;

    private final ShowtimeRepository showtimeRepository;

    private final ShowSeatRepository showSeatRepository;

    private final SeatReservedOutboxFactory seatReservedOutboxFactory;

    private final SeatReservationRejectedOutboxFactory seatReservationRejectedOutboxFactory;

    private final OutboxService outboxService;

    private final Clock clock;

    public SeatReservationRequestedConsumerServiceImpl(
            SeatReservationRequestedMessageValidator messageValidator,
            SeatReservationRequestedPayloadReader payloadReader,
            ProcessedEventRegistrationService processedEventRegistrationService,
            ShowtimeRepository showtimeRepository,
            ShowSeatRepository showSeatRepository,
            SeatReservedOutboxFactory seatReservedOutboxFactory,
            SeatReservationRejectedOutboxFactory seatReservationRejectedOutboxFactory,
            OutboxService outboxService,
            Clock clock) {

        this.messageValidator = messageValidator;
        this.payloadReader = payloadReader;
        this.processedEventRegistrationService = processedEventRegistrationService;
        this.showtimeRepository = showtimeRepository;
        this.showSeatRepository = showSeatRepository;
        this.seatReservedOutboxFactory = seatReservedOutboxFactory;
        this.seatReservationRejectedOutboxFactory = seatReservationRejectedOutboxFactory;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result handle(String partitionKey, OutboxEventMessage message) {

        messageValidator.validate(partitionKey, message);

        SeatReservationRequestedPayload payload = payloadReader.read(message);

        validateTrustedIdentifiers(message, payload);

        boolean registered =
                processedEventRegistrationService.register(
                        message.eventId(),
                        InventoryEventContract.SEAT_RESERVATION_CONSUMER,
                        message.eventType(),
                        message.eventVersion());

        if (!registered) {
            return Result.alreadyProcessed();
        }

        OffsetDateTime now = OffsetDateTime.now(clock);

        ReservationRequestAssessment validation = validateAndNormalize(payload, now);

        if (!validation.valid()) {
            return reject(
                    payload,
                    validation.reason(),
                    validation.message(),
                    validation.affectedSeats(),
                    now,
                    message);
        }

        List<String> seatNumbers = validation.normalizedSeatNumbers();

        Showtime showtime = showtimeRepository.findById(payload.showtimeId()).orElse(null);

        if (showtime == null || showtime.getStatus() != ShowtimeStatus.OPEN_FOR_BOOKING) {

            return reject(
                    payload,
                    SeatReservationRejectionReason.INVALID_REQUEST,
                    INVALID_REQUEST_MESSAGE,
                    seatNumbers,
                    now,
                    message);
        }

        List<ShowSeat> showSeats =
                showSeatRepository.findAllByShowtimeIdAndSeatNumbersForUpdate(
                        payload.showtimeId(), seatNumbers);

        List<String> foundSeatNumbers =
                showSeats.stream()
                        .map(ShowSeat::getSeatNumber)
                        .map(this::normalizeSeatNumber)
                        .toList();

        List<String> missingSeats =
                seatNumbers.stream()
                        .filter(seatNumber -> !foundSeatNumbers.contains(seatNumber))
                        .toList();

        if (!missingSeats.isEmpty()) {
            return reject(
                    payload,
                    SeatReservationRejectionReason.SEAT_NOT_FOUND,
                    SEAT_NOT_FOUND_MESSAGE,
                    missingSeats,
                    now,
                    message);
        }

        List<String> unavailableSeats =
                showSeats.stream()
                        .filter(showSeat -> !canUseExistingOrCreateHold(showSeat, payload, now))
                        .map(ShowSeat::getSeatNumber)
                        .sorted()
                        .toList();

        if (!unavailableSeats.isEmpty()) {
            return reject(
                    payload,
                    SeatReservationRejectionReason.SEAT_UNAVAILABLE,
                    SEAT_UNAVAILABLE_MESSAGE,
                    unavailableSeats,
                    now,
                    message);
        }

        showSeats.stream()
                .filter(showSeat -> showSeat.getStatus() == ShowSeatStatus.AVAILABLE)
                .forEach(
                        showSeat ->
                                showSeat.hold(payload.bookingId(), payload.holdExpiresAt(), now));

        showSeatRepository.saveAll(showSeats);

        OutboxEventEntity outboxEvent =
                seatReservedOutboxFactory.create(
                        payload.bookingId(),
                        payload.showtimeId(),
                        showSeats,
                        now,
                        payload.holdExpiresAt(),
                        message.correlationId(),
                        message.eventId());

        outboxService.save(outboxEvent);

        return Result.reserved(outboxEvent.getId());
    }

    private void validateTrustedIdentifiers(
            OutboxEventMessage message, SeatReservationRequestedPayload payload) {

        if (payload == null
                || payload.bookingId() == null
                || payload.userId() == null
                || payload.showtimeId() == null
                || !payload.bookingId().equals(message.aggregateId())) {

            throw new com.cinema.common.exception.exception.ValidationException(
                    com.cinema.inventory.exception.InventoryErrorCode.EVENT_PAYLOAD_INVALID);
        }

        if (payload.bookingId().version() != 7
                || payload.userId().version() != 7
                || payload.showtimeId().version() != 7) {

            throw new com.cinema.common.exception.exception.ValidationException(
                    com.cinema.inventory.exception.InventoryErrorCode.EVENT_PAYLOAD_INVALID);
        }
    }

    private ReservationRequestAssessment validateAndNormalize(
            SeatReservationRequestedPayload payload, OffsetDateTime now) {

        if (payload.requestedAt() == null
                || payload.holdExpiresAt() == null
                || payload.seats() == null
                || payload.seats().isEmpty()) {

            return ReservationRequestAssessment.rejected(
                    SeatReservationRejectionReason.INVALID_REQUEST,
                    INVALID_REQUEST_MESSAGE,
                    List.of());
        }

        if (!payload.holdExpiresAt().isAfter(now)) {
            return ReservationRequestAssessment.rejected(
                    SeatReservationRejectionReason.RESERVATION_EXPIRED,
                    RESERVATION_EXPIRED_MESSAGE,
                    requestedSeatNumbers(payload));
        }

        if (!payload.holdExpiresAt().isAfter(payload.requestedAt())) {

            return ReservationRequestAssessment.rejected(
                    SeatReservationRejectionReason.INVALID_REQUEST,
                    INVALID_REQUEST_MESSAGE,
                    requestedSeatNumbers(payload));
        }

        List<String> normalizedSeatNumbers = new ArrayList<>();

        for (RequestedSeatPayload seat : payload.seats()) {

            if (seat == null || seat.seatNumber() == null || seat.seatNumber().isBlank()) {

                return ReservationRequestAssessment.rejected(
                        SeatReservationRejectionReason.INVALID_REQUEST,
                        INVALID_REQUEST_MESSAGE,
                        List.of());
            }

            normalizedSeatNumbers.add(normalizeSeatNumber(seat.seatNumber()));
        }

        Set<String> uniqueSeatNumbers = new HashSet<>(normalizedSeatNumbers);

        if (uniqueSeatNumbers.size() != normalizedSeatNumbers.size()) {

            return ReservationRequestAssessment.rejected(
                    SeatReservationRejectionReason.DUPLICATE_SEAT,
                    DUPLICATE_SEAT_MESSAGE,
                    duplicateSeatNumbers(normalizedSeatNumbers));
        }

        List<String> sortedSeatNumbers = uniqueSeatNumbers.stream().sorted().toList();

        return ReservationRequestAssessment.accepted(sortedSeatNumbers);
    }

    private boolean canUseExistingOrCreateHold(
            ShowSeat showSeat, SeatReservationRequestedPayload payload, OffsetDateTime now) {

        if (showSeat.getStatus() == ShowSeatStatus.AVAILABLE) {

            return true;
        }

        return showSeat.isHeldBy(payload.bookingId())
                && !showSeat.hasExpired(now)
                && payload.holdExpiresAt().equals(showSeat.getHoldExpiresAt());
    }

    private Result reject(
            SeatReservationRequestedPayload payload,
            SeatReservationRejectionReason reason,
            String message,
            List<String> affectedSeats,
            OffsetDateTime rejectedAt,
            OutboxEventMessage sourceMessage) {

        OutboxEventEntity outboxEvent =
                seatReservationRejectedOutboxFactory.create(
                        payload.bookingId(),
                        payload.showtimeId(),
                        reason,
                        message,
                        affectedSeats,
                        rejectedAt,
                        sourceMessage.correlationId(),
                        sourceMessage.eventId());

        outboxService.save(outboxEvent);

        return Result.rejected(outboxEvent.getId());
    }

    private List<String> requestedSeatNumbers(SeatReservationRequestedPayload payload) {

        if (payload.seats() == null) {
            return List.of();
        }

        return payload.seats().stream()
                .filter(seat -> seat != null)
                .map(RequestedSeatPayload::seatNumber)
                .filter(seatNumber -> seatNumber != null && !seatNumber.isBlank())
                .map(this::normalizeSeatNumber)
                .sorted()
                .toList();
    }

    private List<String> duplicateSeatNumbers(List<String> seatNumbers) {

        Set<String> encountered = new HashSet<>();
        Set<String> duplicates = new HashSet<>();

        seatNumbers.forEach(
                seatNumber -> {
                    if (!encountered.add(seatNumber)) {
                        duplicates.add(seatNumber);
                    }
                });

        return duplicates.stream().sorted().toList();
    }

    private String normalizeSeatNumber(String seatNumber) {

        return seatNumber.trim().toUpperCase(Locale.ROOT);
    }

    private record ReservationRequestAssessment(
            boolean valid,
            List<String> normalizedSeatNumbers,
            SeatReservationRejectionReason reason,
            String message,
            List<String> affectedSeats) {

        private ReservationRequestAssessment {

            normalizedSeatNumbers =
                    normalizedSeatNumbers == null ? List.of() : List.copyOf(normalizedSeatNumbers);

            affectedSeats = affectedSeats == null ? List.of() : List.copyOf(affectedSeats);
        }

        static ReservationRequestAssessment accepted(List<String> normalizedSeatNumbers) {

            return new ReservationRequestAssessment(
                    true, normalizedSeatNumbers, null, null, List.of());
        }

        static ReservationRequestAssessment rejected(
                SeatReservationRejectionReason reason, String message, List<String> affectedSeats) {

            return new ReservationRequestAssessment(
                    false, List.of(), reason, message, affectedSeats);
        }
    }
}
