package com.cinema.inventory.event;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.event.payload.PolicySeatReservedPayload;
import com.cinema.inventory.event.payload.ReservedSeatPayload;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.service.pricing.QuoteReservationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.UUID;

@Component
public class PolicySeatReservedOutboxFactory {

    private final ObjectMapper objectMapper;

    public PolicySeatReservedOutboxFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OutboxEventEntity create(
            UUID bookingId,
            QuoteReservationService.Reservation reservation,
            UUID correlationId,
            UUID causationId) {

        var quote = reservation.quote();

        var seats =
                quote.items().stream()
                        .sorted(
                                Comparator.comparing(
                                        com.cinema.common.api.pricing.BookingQuoteResponse.Line
                                                ::seatNumber))
                        .map(
                                line ->
                                        new ReservedSeatPayload(
                                                line.showSeatId(),
                                                line.seatNumber(),
                                                SeatType.valueOf(line.seatType()),
                                                new BigDecimal(line.amountVnd())))
                        .toList();

        var payload =
                new PolicySeatReservedPayload(
                        bookingId,
                        quote.showtimeId(),
                        seats,
                        new BigDecimal(quote.totalAmountVnd()),
                        quote.currency(),
                        reservation.heldAt(),
                        reservation.holdExpiresAt(),
                        quote);

        return new OutboxEventEntity(
                UuidGenerator.next(),
                AggregateType.BOOKING,
                bookingId,
                InventoryEventContract.SEAT_RESERVED,
                InventoryEventContract.SEAT_RESERVED_POLICY_VERSION,
                InventoryEventContract.SEAT_RESERVED,
                bookingId.toString(),
                reservation.heldAt(),
                correlationId,
                causationId,
                serialize(payload),
                reservation.heldAt());
    }

    private String serialize(PolicySeatReservedPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    InventoryErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, exception);
        }
    }
}
