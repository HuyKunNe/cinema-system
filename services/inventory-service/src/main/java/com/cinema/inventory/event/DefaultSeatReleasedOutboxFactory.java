package com.cinema.inventory.event;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.event.payload.SeatReleasedPayload;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class DefaultSeatReleasedOutboxFactory implements SeatReleasedOutboxFactory {

    private final ObjectMapper objectMapper;

    public DefaultSeatReleasedOutboxFactory(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEventEntity create(
            UUID bookingId,
            UUID showtimeId,
            List<ShowSeat> releasedSeats,
            String reason,
            OffsetDateTime releasedAt,
            UUID correlationId,
            UUID causationId) {

        List<UUID> releasedSeatIds =
                releasedSeats.stream()
                        .map(ShowSeat::getId)
                        .sorted(Comparator.naturalOrder())
                        .toList();

        SeatReleasedPayload payload =
                new SeatReleasedPayload(bookingId, showtimeId, releasedSeatIds, reason, releasedAt);

        return new OutboxEventEntity(
                UuidGenerator.next(),
                AggregateType.BOOKING,
                bookingId,
                InventoryEventContract.SEAT_RELEASED,
                InventoryEventContract.SEAT_RELEASED_VERSION,
                InventoryEventContract.SEAT_RELEASED,
                bookingId.toString(),
                releasedAt,
                correlationId,
                causationId,
                serialize(payload),
                releasedAt);
    }

    private String serialize(SeatReleasedPayload payload) {

        try {
            return objectMapper.writeValueAsString(payload);

        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    InventoryErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, exception);
        }
    }
}
