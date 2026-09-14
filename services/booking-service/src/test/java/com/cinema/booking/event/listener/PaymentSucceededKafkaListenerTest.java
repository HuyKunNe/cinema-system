package com.cinema.booking.event.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.serialization.PaymentResultMessageReader;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.service.PaymentSucceededConsumerService;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

@ExtendWith(MockitoExtension.class)
class PaymentSucceededKafkaListenerTest {

    private static final String SERIALIZED_MESSAGE =
            """
            {
              "eventType": "payment-succeeded"
            }
            """;

    @Mock private PaymentResultMessageReader messageReader;

    @Mock private PaymentSucceededConsumerService consumerService;

    private PaymentSucceededKafkaListener listener;

    @BeforeEach
    void setUp() {

        listener = new PaymentSucceededKafkaListener(messageReader, consumerService);
    }

    @Test
    void messageShouldBeReadBeforeConsumerServiceIsCalled() {

        UUID bookingId = UuidGenerator.next();

        String partitionKey = bookingId.toString();

        OutboxEventMessage message = message();

        when(messageReader.read(SERIALIZED_MESSAGE)).thenReturn(message);

        when(consumerService.handle(partitionKey, message))
                .thenReturn(PaymentSucceededConsumerService.Result.confirmed());

        listener.consume(partitionKey, SERIALIZED_MESSAGE);

        InOrder order = inOrder(messageReader, consumerService);

        order.verify(messageReader).read(SERIALIZED_MESSAGE);

        order.verify(consumerService).handle(partitionKey, message);

        order.verifyNoMoreInteractions();
    }

    @Test
    void readerValidationFailureShouldPropagateToKafkaErrorHandler() {

        ValidationException failure =
                new ValidationException(BookingErrorCode.EVENT_MESSAGE_INVALID);

        when(messageReader.read(SERIALIZED_MESSAGE)).thenThrow(failure);

        assertThatThrownBy(() -> listener.consume("booking-key", SERIALIZED_MESSAGE))
                .isSameAs(failure);

        verifyNoInteractions(consumerService);
    }

    @Test
    void businessConflictShouldPropagateToKafkaErrorHandler() {

        UUID bookingId = UuidGenerator.next();

        String partitionKey = bookingId.toString();

        OutboxEventMessage message = message();

        ConflictException failure = new ConflictException(BookingErrorCode.BOOKING_NOT_RESERVED);

        when(messageReader.read(SERIALIZED_MESSAGE)).thenReturn(message);

        when(consumerService.handle(partitionKey, message)).thenThrow(failure);

        assertThatThrownBy(() -> listener.consume(partitionKey, SERIALIZED_MESSAGE))
                .isSameAs(failure);
    }

    private OutboxEventMessage message() {

        UUID paymentId = UuidGenerator.next();

        return new OutboxEventMessage(
                UuidGenerator.next(),
                paymentId,
                BookingEventContract.PAYMENT_AGGREGATE_TYPE,
                BookingEventContract.PAYMENT_SUCCEEDED,
                BookingEventContract.PAYMENT_SUCCEEDED_VERSION,
                OffsetDateTime.parse("2026-09-15T10:00:00Z"),
                BookingEventContract.PAYMENT_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                JsonNodeFactory.instance.objectNode());
    }
}
