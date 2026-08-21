package com.cinema.inventory.event;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
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
import java.util.List;
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
                        .sorted(Comparator.comparing(ShowSeat::getSeatNumber))
                        .map(this::toPayload)
                        .toList();

        BigDecimal totalAmount =
                seats.stream()
                        .map(ReservedSeatPayload::price)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        SeatReservedPayload payload =
                new SeatReservedPayload(
                        bookingId,
                        showtimeId,
                        seats,
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

    private ReservedSeatPayload toPayload(ShowSeat showSeat) {

        return new ReservedSeatPayload(
                showSeat.getId(),
                showSeat.getSeatNumber(),
                showSeat.getSeatType(),
                showSeat.getPrice());
    }

    private String serialize(SeatReservedPayload payload) {

        try {
            return objectMapper.writeValueAsString(payload);

        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    InventoryErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, exception);
        }
    }
}
