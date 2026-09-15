package com.cinema.inventory.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.service.OutboxService;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.SeatReleasedOutboxFactory;
import com.cinema.inventory.event.payload.SeatReleaseRequestedPayload;
import com.cinema.inventory.event.serialization.SeatReleaseRequestedPayloadReader;
import com.cinema.inventory.event.validation.SeatReleaseRequestedMessageValidator;
import com.cinema.inventory.event.validation.SeatReleaseRequestedPayloadValidator;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.service.ProcessedEventRegistrationService;
import com.cinema.inventory.service.SeatReleaseRequestedConsumerService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SeatReleaseRequestedConsumerServiceImpl
        implements SeatReleaseRequestedConsumerService {

    private final SeatReleaseRequestedMessageValidator messageValidator;

    private final SeatReleaseRequestedPayloadReader payloadReader;

    private final SeatReleaseRequestedPayloadValidator payloadValidator;

    private final ProcessedEventRegistrationService processedEventRegistrationService;

    private final ShowSeatRepository showSeatRepository;

    private final SeatReleasedOutboxFactory seatReleasedOutboxFactory;

    private final OutboxService outboxService;

    private final Clock clock;

    public SeatReleaseRequestedConsumerServiceImpl(
            SeatReleaseRequestedMessageValidator messageValidator,
            SeatReleaseRequestedPayloadReader payloadReader,
            SeatReleaseRequestedPayloadValidator payloadValidator,
            ProcessedEventRegistrationService processedEventRegistrationService,
            ShowSeatRepository showSeatRepository,
            SeatReleasedOutboxFactory seatReleasedOutboxFactory,
            OutboxService outboxService,
            Clock clock) {

        this.messageValidator = messageValidator;
        this.payloadReader = payloadReader;
        this.payloadValidator = payloadValidator;
        this.processedEventRegistrationService = processedEventRegistrationService;
        this.showSeatRepository = showSeatRepository;
        this.seatReleasedOutboxFactory = seatReleasedOutboxFactory;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result handle(String partitionKey, OutboxEventMessage message) {

        messageValidator.validate(partitionKey, message);

        SeatReleaseRequestedPayload payload = payloadReader.read(message);

        payloadValidator.validate(partitionKey, message, payload);

        boolean registered =
                processedEventRegistrationService.register(
                        message.eventId(),
                        InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER,
                        message.eventType(),
                        message.eventVersion());

        if (!registered) {
            return Result.alreadyProcessed();
        }

        List<UUID> sortedSeatIds = payload.seatIds().stream().sorted().toList();

        List<ShowSeat> showSeats =
                showSeatRepository.findAllByShowtimeIdAndIdsForUpdate(
                        payload.showtimeId(), sortedSeatIds);

        validateCompleteSeatSet(sortedSeatIds, showSeats);

        /*
         * Validate every entity before mutating any entity. This prevents
         * partial in-memory release when one requested seat is invalid.
         */
        showSeats.forEach(showSeat -> validateHeldOwnership(payload.bookingId(), showSeat));

        showSeats.forEach(showSeat -> showSeat.release(payload.bookingId()));

        showSeatRepository.saveAll(showSeats);

        OffsetDateTime releasedAt = OffsetDateTime.now(clock);

        OutboxEventEntity outboxEvent =
                seatReleasedOutboxFactory.create(
                        payload.bookingId(),
                        payload.showtimeId(),
                        showSeats,
                        payload.reason(),
                        releasedAt,
                        message.correlationId(),
                        message.eventId());

        outboxService.save(outboxEvent);

        return Result.released(outboxEvent.getId());
    }

    private void validateCompleteSeatSet(List<UUID> expectedSeatIds, List<ShowSeat> showSeats) {

        if (showSeats == null || showSeats.size() != expectedSeatIds.size()) {
            throw mismatch();
        }

        Set<UUID> expectedIds = new HashSet<>(expectedSeatIds);

        boolean unexpectedSeat =
                showSeats.stream()
                        .anyMatch(
                                showSeat ->
                                        showSeat == null
                                                || showSeat.getId() == null
                                                || !expectedIds.contains(showSeat.getId()));

        if (unexpectedSeat) {
            throw mismatch();
        }
    }

    private void validateHeldOwnership(UUID bookingId, ShowSeat showSeat) {

        if (!showSeat.isHeldBy(bookingId)) {
            throw mismatch();
        }
    }

    private ConflictException mismatch() {

        return new ConflictException(InventoryErrorCode.SEAT_RELEASE_MISMATCH);
    }
}
