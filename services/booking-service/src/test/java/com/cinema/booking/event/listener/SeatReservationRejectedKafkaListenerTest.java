package com.cinema.booking.event.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.booking.event.serialization.SeatReservationRejectedMessageReader;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.service.SeatReservationRejectedConsumerService;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class SeatReservationRejectedKafkaListenerTest {

    private static final String SERIALIZED_MESSAGE =
            """
            {
              "eventType": "seat-reservation-rejected"
            }
            """;

    @Mock private SeatReservationRejectedMessageReader messageReader;

    @Mock private SeatReservationRejectedConsumerService consumerService;

    private SeatReservationRejectedKafkaListener listener;

    @BeforeEach
    void setUp() {

        listener = new SeatReservationRejectedKafkaListener(messageReader, consumerService);
    }

    @Test
    void messageShouldBeReadBeforeConsumerServiceIsCalled() {

        UUID bookingId = UuidGenerator.next();

        String partitionKey = bookingId.toString();

        OutboxEventMessage message = message(bookingId);

        when(messageReader.read(SERIALIZED_MESSAGE)).thenReturn(message);

        when(consumerService.handle(partitionKey, message))
                .thenReturn(SeatReservationRejectedConsumerService.Result.rejected());

        listener.consume(partitionKey, SERIALIZED_MESSAGE);

        InOrder order = inOrder(messageReader, consumerService);

        order.verify(messageReader).read(SERIALIZED_MESSAGE);

        order.verify(consumerService).handle(partitionKey, message);

        order.verifyNoMoreInteractions();
    }

    @Test
    void duplicateResultShouldBeAcceptedWithoutThrowing() {

        UUID bookingId = UuidGenerator.next();

        String partitionKey = bookingId.toString();

        OutboxEventMessage message = message(bookingId);

        when(messageReader.read(SERIALIZED_MESSAGE)).thenReturn(message);

        when(consumerService.handle(partitionKey, message))
                .thenReturn(SeatReservationRejectedConsumerService.Result.alreadyProcessed());

        listener.consume(partitionKey, SERIALIZED_MESSAGE);

        verify(consumerService).handle(partitionKey, message);
    }

    @Test
    void messageReaderFailureShouldPropagate() {

        ValidationException exception =
                new ValidationException(BookingErrorCode.EVENT_MESSAGE_INVALID);

        when(messageReader.read(SERIALIZED_MESSAGE)).thenThrow(exception);

        assertThatThrownBy(() -> listener.consume("booking-key", SERIALIZED_MESSAGE))
                .isSameAs(exception);

        verifyNoInteractions(consumerService);
    }

    @Test
    void payloadValidationFailureShouldPropagate() {

        UUID bookingId = UuidGenerator.next();

        String partitionKey = bookingId.toString();

        OutboxEventMessage message = message(bookingId);

        ValidationException exception =
                new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);

        when(messageReader.read(SERIALIZED_MESSAGE)).thenReturn(message);

        when(consumerService.handle(partitionKey, message)).thenThrow(exception);

        assertThatThrownBy(() -> listener.consume(partitionKey, SERIALIZED_MESSAGE))
                .isSameAs(exception);
    }

    @Test
    void businessConflictShouldPropagateToRetryPolicy() {

        UUID bookingId = UuidGenerator.next();

        String partitionKey = bookingId.toString();

        OutboxEventMessage message = message(bookingId);

        ConflictException exception = new ConflictException(BookingErrorCode.BOOKING_NOT_PENDING);

        when(messageReader.read(SERIALIZED_MESSAGE)).thenReturn(message);

        when(consumerService.handle(partitionKey, message)).thenThrow(exception);

        assertThatThrownBy(() -> listener.consume(partitionKey, SERIALIZED_MESSAGE))
                .isSameAs(exception);
    }

    private OutboxEventMessage message(UUID bookingId) {

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                "BOOKING",
                "seat-reservation-rejected",
                "1",
                OffsetDateTime.parse("2026-08-24T10:00:00Z"),
                "inventory-service",
                UuidGenerator.next(),
                null,
                JsonNodeFactory.instance.objectNode().put("bookingId", bookingId.toString()));
    }
}
