package com.cinema.payment.event;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.event.payload.PaymentFailedPayload;
import com.cinema.payment.exception.PaymentErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class DefaultPaymentFailedOutboxFactory implements PaymentFailedOutboxFactory {

    private static final boolean TERMINAL_RETRYABLE = false;

    private final ObjectMapper objectMapper;

    public DefaultPaymentFailedOutboxFactory(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEventEntity create(Payment payment) {

        requireFailedOrExpiredPayment(payment);

        OffsetDateTime failedAt = payment.getCompletedAt();

        PaymentFailedPayload payload =
                new PaymentFailedPayload(
                        payment.getId(),
                        payment.getBookingId(),
                        payment.getFailureCode(),
                        payment.getFailureMessage(),
                        failedAt,
                        TERMINAL_RETRYABLE);

        UUID eventId = UuidGenerator.next();

        return new OutboxEventEntity(
                eventId,
                AggregateType.PAYMENT,
                payment.getId(),
                PaymentEventContract.PAYMENT_FAILED,
                PaymentEventContract.PAYMENT_FAILED_VERSION,
                PaymentEventContract.PAYMENT_FAILED,
                payment.getBookingId().toString(),
                failedAt,
                payment.getCorrelationId(),
                payment.getSourceEventId(),
                serialize(payload),
                failedAt);
    }

    private static void requireFailedOrExpiredPayment(Payment payment) {

        if (payment == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_REQUIRED);
        }

        if (payment.getStatus() != PaymentStatus.FAILED
                && payment.getStatus() != PaymentStatus.EXPIRED) {

            throw new ConflictException(
                    PaymentErrorCode.PAYMENT_RESULT_OUTBOX_NOT_APPLICABLE);
        }
    }

    private String serialize(PaymentFailedPayload payload) {

        try {
            return objectMapper.writeValueAsString(payload);

        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    PaymentErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, exception);
        }
    }
}
