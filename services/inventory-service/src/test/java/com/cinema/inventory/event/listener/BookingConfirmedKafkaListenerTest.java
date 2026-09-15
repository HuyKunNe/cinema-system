package com.cinema.inventory.event.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.serialization.BookingConfirmedMessageReader;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.service.BookingConfirmedConsumerService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class BookingConfirmedKafkaListenerTest {

    private final BookingConfirmedMessageReader messageReader =
            mock(BookingConfirmedMessageReader.class);

    private final BookingConfirmedConsumerService consumerService =
            mock(BookingConfirmedConsumerService.class);

    private final BookingConfirmedKafkaListener listener =
            new BookingConfirmedKafkaListener(messageReader, consumerService);

    @Test
    void shouldDelegateCanonicalMessageToConsumerService() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message = message(bookingId);

        String serializedMessage = "{\"eventId\":\"event\"}";

        when(messageReader.read(serializedMessage)).thenReturn(message);

        listener.consume(bookingId.toString(), serializedMessage);

        verify(messageReader).read(serializedMessage);

        verify(consumerService).handle(bookingId.toString(), message);
    }

    @Test
    void malformedMessageShouldPropagateValidationException() {

        UUID bookingId = UuidGenerator.next();

        String serializedMessage = "{\"payload\":";

        ValidationException failure =
                new ValidationException(InventoryErrorCode.EVENT_MESSAGE_INVALID);

        when(messageReader.read(serializedMessage)).thenThrow(failure);

        assertThatThrownBy(() -> listener.consume(bookingId.toString(), serializedMessage))
                .isSameAs(failure);

        verifyNoInteractions(consumerService);
    }

    @Test
    void consumerFailureShouldPropagateToKafkaErrorHandler() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message = message(bookingId);

        String serializedMessage = "{\"eventId\":\"event\"}";

        ConflictException failure =
                new ConflictException(InventoryErrorCode.SEAT_NOT_HELD_BY_BOOKING);

        when(messageReader.read(serializedMessage)).thenReturn(message);

        when(consumerService.handle(bookingId.toString(), message)).thenThrow(failure);

        assertThatThrownBy(() -> listener.consume(bookingId.toString(), serializedMessage))
                .isSameAs(failure);
    }

    private OutboxEventMessage message(UUID bookingId) {

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.BOOKING_CONFIRMED,
                InventoryEventContract.BOOKING_CONFIRMED_VERSION,
                OffsetDateTime.parse("2026-09-15T10:00:00Z"),
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                JsonNodeFactory.instance.objectNode());
    }
}
