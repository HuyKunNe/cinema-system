package com.cinema.payment.event;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.event.payload.PaymentSucceededPayload;
import com.cinema.payment.exception.PaymentErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class DefaultPaymentSucceededOutboxFactory implements PaymentSucceededOutboxFactory {

    private final ObjectMapper objectMapper;

    public DefaultPaymentSucceededOutboxFactory(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEventEntity create(Payment payment) {

        requireSucceededPayment(payment);

        OffsetDateTime paidAt = payment.getCompletedAt();

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        payment.getId(),
                        payment.getBookingId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getProvider(),
                        payment.getProviderReference(),
                        paidAt);

        UUID eventId = UuidGenerator.next();

        return new OutboxEventEntity(
                eventId,
                AggregateType.PAYMENT,
                payment.getId(),
                PaymentEventContract.PAYMENT_SUCCEEDED,
                PaymentEventContract.PAYMENT_SUCCEEDED_VERSION,
                PaymentEventContract.PAYMENT_SUCCEEDED,
                payment.getBookingId().toString(),
                paidAt,
                payment.getCorrelationId(),
                payment.getSourceEventId(),
                serialize(payload),
                paidAt);
    }

    private static void requireSucceededPayment(Payment payment) {

        if (payment == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_REQUIRED);
        }

        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new ConflictException(PaymentErrorCode.PAYMENT_RESULT_OUTBOX_NOT_APPLICABLE);
        }
    }

    private String serialize(PaymentSucceededPayload payload) {

        try {
            return objectMapper.writeValueAsString(payload);

        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    PaymentErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, exception);
        }
    }
}
