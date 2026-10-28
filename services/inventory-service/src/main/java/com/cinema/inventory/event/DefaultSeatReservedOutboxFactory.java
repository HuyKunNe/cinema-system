package com.cinema.inventory.event;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.event.payload.ReservedSeatPayload;
import com.cinema.inventory.event.payload.SeatReservedPayload;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class DefaultSeatReservedOutboxFactory implements SeatReservedOutboxFactory {

    private final ObjectMapper objectMapper;

    public DefaultSeatReservedOutboxFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEventEntity create(
            UUID bookingId,
            UUID showtimeId,
            List<ShowSeat> showSeats,
            OffsetDateTime heldAt,
            OffsetDateTime holdExpiresAt,
            UUID correlationId,
            UUID causationId) {

        List<ReservedSeatPayload> seats =
                showSeats.stream()
                        .map(DefaultSeatReservedOutboxFactory::toDefaultSnapshot)
                        .toList();

        return createFromSnapshots(
                bookingId, showtimeId, seats, heldAt, holdExpiresAt, correlationId, causationId);
    }

    @Override
    public OutboxEventEntity createFromSnapshots(
            UUID bookingId,
            UUID showtimeId,
            List<ReservedSeatPayload> seats,
            OffsetDateTime heldAt,
            OffsetDateTime holdExpiresAt,
            UUID correlationId,
            UUID causationId) {

        validateSnapshots(seats);

        List<ReservedSeatPayload> snapshots =
                seats.stream()
                        .sorted(Comparator.comparing(ReservedSeatPayload::seatNumber))
                        .toList();

        // Each COUPLE snapshot already contains the entire pair price.
        BigDecimal totalAmount =
                snapshots.stream()
                        .map(ReservedSeatPayload::price)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        SeatReservedPayload payload =
                new SeatReservedPayload(
                        bookingId,
                        showtimeId,
                        snapshots,
                        totalAmount,
                        InventoryEventContract.CURRENCY_VND,
                        heldAt,
                        holdExpiresAt);

        return new OutboxEventEntity(
                UuidGenerator.next(),
                AggregateType.BOOKING,
                bookingId,
                InventoryEventContract.SEAT_RESERVED,
                InventoryEventContract.SEAT_RESERVED_VERSION,
                InventoryEventContract.SEAT_RESERVED,
                bookingId.toString(),
                heldAt,
                correlationId,
                causationId,
                serialize(payload),
                heldAt);
    }

    private static void validateSnapshots(List<ReservedSeatPayload> seats) {

        if (seats == null || seats.isEmpty()) {
            throw invalidPayload();
        }

        Set<UUID> identifiers = new HashSet<>();
        Set<String> numbers = new HashSet<>();

        for (ReservedSeatPayload seat : seats) {
            if (seat == null
                    || seat.inventorySeatId() == null
                    || seat.seatNumber() == null
                    || seat.seatNumber().isBlank()
                    || seat.seatNumber().length() > 20
                    || seat.seatType() == null
                    || seat.price() == null
                    || seat.price().signum() < 0) {

                throw invalidPayload();
            }

            if (seat.ticketGroupName() != null
                    && (seat.ticketGroupId() == null
                            || seat.ticketGroupName().isBlank()
                            || seat.ticketGroupName().length() > 120)) {

                throw invalidPayload();
            }

            String number = seat.seatNumber().trim().toUpperCase(Locale.ROOT);

            if (!identifiers.add(seat.inventorySeatId()) || !numbers.add(number)) {

                throw invalidPayload();
            }
        }
    }

    private String serialize(SeatReservedPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);

        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    InventoryErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, exception);
        }
    }

    private static ValidationException invalidPayload() {
        return new ValidationException(InventoryErrorCode.EVENT_PAYLOAD_INVALID);
    }

    private static ReservedSeatPayload toDefaultSnapshot(ShowSeat seat) {

        var selected = seat.getShowtime().getPricingPolicyVersion();

        if (selected == null) {
            return new ReservedSeatPayload(
                    seat.getId(), seat.getSeatNumber(), seat.getSeatType(), seat.getPrice());
        }

        var defaults =
                selected.getPricingTicketGroups().stream()
                        .filter(group -> group.isDefaultGroup())
                        .toList();

        if (defaults.size() != 1) {
            throw invalidPayload();
        }

        var group = defaults.getFirst();

        return new ReservedSeatPayload(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getSeatType(),
                seat.getPrice(),
                group.getId(),
                group.getName());
    }
}
