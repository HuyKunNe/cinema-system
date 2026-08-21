package com.cinema.inventory.event;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.inventory.event.payload.SeatReservationRejectedPayload;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class DefaultSeatReservationRejectedOutboxFactory
        implements SeatReservationRejectedOutboxFactory {

    private final ObjectMapper objectMapper;

    public DefaultSeatReservationRejectedOutboxFactory(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEventEntity create(
            UUID bookingId,
            UUID showtimeId,
            SeatReservationRejectionReason reason,
            String message,
            List<String> unavailableSeats,
            OffsetDateTime rejectedAt,
            UUID correlationId,
            UUID causationId) {

        List<String> normalizedUnavailableSeats =
                unavailableSeats == null
                        ? List.of()
                        : unavailableSeats.stream().sorted(Comparator.naturalOrder()).toList();

        SeatReservationRejectedPayload payload =
                new SeatReservationRejectedPayload(
                        bookingId,
                        showtimeId,
                        reason,
                        message,
                        normalizedUnavailableSeats,
                        rejectedAt);

        return new OutboxEventEntity(
                UuidGenerator.next(),
                AggregateType.BOOKING,
                bookingId,
                InventoryEventContract.SEAT_RESERVATION_REJECTED,
                InventoryEventContract.SEAT_RESERVATION_REJECTED_VERSION,
                InventoryEventContract.SEAT_RESERVATION_REJECTED,
                bookingId.toString(),
                rejectedAt,
                correlationId,
                causationId,
                serialize(payload),
                rejectedAt);
    }

    private String serialize(SeatReservationRejectedPayload payload) {

        try {
            return objectMapper.writeValueAsString(payload);

        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    InventoryErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, exception);
        }
    }
}
