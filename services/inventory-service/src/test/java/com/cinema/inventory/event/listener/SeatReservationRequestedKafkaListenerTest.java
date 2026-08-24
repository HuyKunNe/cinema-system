package com.cinema.inventory.event.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.serialization.SeatReservationRequestedMessageReader;
import com.cinema.inventory.service.SeatReservationRequestedConsumerService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class SeatReservationRequestedKafkaListenerTest {

    private final SeatReservationRequestedMessageReader messageReader =
            mock(SeatReservationRequestedMessageReader.class);

    private final SeatReservationRequestedConsumerService consumerService =
            mock(SeatReservationRequestedConsumerService.class);

    private final SeatReservationRequestedKafkaListener listener =
            new SeatReservationRequestedKafkaListener(messageReader, consumerService);

    @Test
    void shouldDelegateCanonicalMessageToConsumerService() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                new OutboxEventMessage(
                        UuidGenerator.next(),
                        bookingId,
                        "BOOKING",
                        "seat-reservation-requested",
                        "1",
                        OffsetDateTime.parse("2026-08-24T10:00:00Z"),
                        "booking-service",
                        UuidGenerator.next(),
                        null,
                        JsonNodeFactory.instance.objectNode());

        String serializedMessage = "{\"eventId\":\"event\"}";

        when(messageReader.read(serializedMessage)).thenReturn(message);

        listener.consume(bookingId.toString(), serializedMessage);

        verify(messageReader).read(serializedMessage);

        verify(consumerService).handle(bookingId.toString(), message);
    }
}
