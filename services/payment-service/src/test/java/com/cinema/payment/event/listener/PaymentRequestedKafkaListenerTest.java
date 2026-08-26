package com.cinema.payment.event.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.payment.event.serialization.PaymentRequestedMessageReader;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.service.PaymentRequestedConsumerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentRequestedKafkaListenerTest {

    private static final String PARTITION_KEY = "ca2fb6c6-7fde-47d7-9bdd-a6f7516358bb";

    private static final String SERIALIZED_MESSAGE =
            """
            {
              "eventType": "payment-requested"
            }
            """;

    @Mock private PaymentRequestedMessageReader messageReader;

    @Mock private PaymentRequestedConsumerService consumerService;

    @Mock private OutboxEventMessage message;

    private PaymentRequestedKafkaListener listener;

    @BeforeEach
    void setUp() {

        listener = new PaymentRequestedKafkaListener(messageReader, consumerService);
    }

    @Test
    void messageShouldBeReadBeforeConsumerServiceIsCalled() {

        when(messageReader.read(SERIALIZED_MESSAGE)).thenReturn(message);

        listener.consume(PARTITION_KEY, SERIALIZED_MESSAGE);

        InOrder order = inOrder(messageReader, consumerService);

        order.verify(messageReader).read(SERIALIZED_MESSAGE);

        order.verify(consumerService).handle(PARTITION_KEY, message);

        order.verifyNoMoreInteractions();
    }

    @Test
    void readerValidationFailureShouldPropagateToKafkaErrorHandler() {

        ValidationException exception =
                new ValidationException(PaymentErrorCode.EVENT_MESSAGE_INVALID);

        when(messageReader.read(SERIALIZED_MESSAGE)).thenThrow(exception);

        assertThatThrownBy(() -> listener.consume(PARTITION_KEY, SERIALIZED_MESSAGE))
                .isSameAs(exception);

        verifyNoInteractions(consumerService);
    }

    @Test
    void consumerFailureShouldPropagateToKafkaErrorHandler() {

        ValidationException exception =
                new ValidationException(PaymentErrorCode.EVENT_PAYLOAD_INVALID);

        when(messageReader.read(SERIALIZED_MESSAGE)).thenReturn(message);

        when(consumerService.handle(PARTITION_KEY, message)).thenThrow(exception);

        assertThatThrownBy(() -> listener.consume(PARTITION_KEY, SERIALIZED_MESSAGE))
                .isSameAs(exception);
    }
}
