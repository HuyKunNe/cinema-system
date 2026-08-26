package com.cinema.payment.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.payment.config.PaymentProperties;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.event.PaymentEventContract;
import com.cinema.payment.event.payload.PaymentRequestedPayload;
import com.cinema.payment.event.serialization.PaymentRequestedPayloadReader;
import com.cinema.payment.event.validation.PaymentRequestedMessageValidator;
import com.cinema.payment.event.validation.PaymentRequestedPayloadValidator;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.service.PaymentRequestedConsumerService;
import com.cinema.payment.service.ProcessedEventRegistrationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentRequestedConsumerServiceImpl implements PaymentRequestedConsumerService {

    private static final String CHARGE_KEY_PREFIX = "charge:";

    private final PaymentRequestedMessageValidator messageValidator;

    private final PaymentRequestedPayloadReader payloadReader;

    private final PaymentRequestedPayloadValidator payloadValidator;

    private final ProcessedEventRegistrationService processedEventRegistrationService;

    private final PaymentRepository paymentRepository;

    private final PaymentTransactionRepository transactionRepository;

    private final PaymentProperties paymentProperties;

    private final Clock clock;

    public PaymentRequestedConsumerServiceImpl(
            PaymentRequestedMessageValidator messageValidator,
            PaymentRequestedPayloadReader payloadReader,
            PaymentRequestedPayloadValidator payloadValidator,
            ProcessedEventRegistrationService processedEventRegistrationService,
            PaymentRepository paymentRepository,
            PaymentTransactionRepository transactionRepository,
            PaymentProperties paymentProperties,
            Clock clock) {

        this.messageValidator = messageValidator;
        this.payloadReader = payloadReader;
        this.payloadValidator = payloadValidator;
        this.processedEventRegistrationService = processedEventRegistrationService;
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.paymentProperties = paymentProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result handle(String partitionKey, OutboxEventMessage message) {

        messageValidator.validate(partitionKey, message);

        PaymentRequestedPayload payload = payloadReader.read(message);

        payloadValidator.validate(message, payload);

        boolean registered =
                processedEventRegistrationService.register(
                        message.eventId(),
                        PaymentEventContract.PAYMENT_REQUESTED_CONSUMER,
                        message.eventType(),
                        message.eventVersion());

        if (!registered) {
            return Result.alreadyProcessed();
        }

        Optional<Payment> existingPayment =
                paymentRepository.findByBookingIdAndPaymentAttempt(
                        payload.bookingId(), payload.paymentAttempt());

        if (existingPayment.isPresent()) {

            Payment payment = existingPayment.orElseThrow();

            validateDuplicateConsistency(payment, message, payload);

            return Result.existing(payment.getId());
        }

        return createPayment(message, payload);
    }

    private Result createPayment(OutboxEventMessage message, PaymentRequestedPayload payload) {

        OffsetDateTime now = OffsetDateTime.now(clock);

        Payment payment =
                new Payment(
                        payload.bookingId(),
                        payload.userId(),
                        payload.paymentAttempt(),
                        payload.amount(),
                        payload.currency(),
                        paymentProperties.provider(),
                        payload.holdExpiresAt(),
                        payload.requestedAt(),
                        message.eventId(),
                        message.correlationId());

        if (!payload.holdExpiresAt().isAfter(now)) {

            payment.expire(now);

            Payment expiredPayment = paymentRepository.save(payment);

            return Result.expired(expiredPayment.getId());
        }

        Payment savedPayment = paymentRepository.save(payment);

        PaymentTransaction transaction =
                new PaymentTransaction(
                        savedPayment.getId(),
                        savedPayment.getProvider(),
                        PaymentTransactionType.CHARGE,
                        1,
                        savedPayment.getAmount(),
                        savedPayment.getCurrency(),
                        createChargeIdempotencyKey(savedPayment.getId()),
                        now);

        transactionRepository.save(transaction);

        return Result.created(savedPayment.getId());
    }

    private static String createChargeIdempotencyKey(UUID paymentId) {

        return CHARGE_KEY_PREFIX + paymentId;
    }

    private static void validateDuplicateConsistency(
            Payment existing, OutboxEventMessage message, PaymentRequestedPayload payload) {

        boolean consistent =
                existing.getBookingId().equals(payload.bookingId())
                        && existing.getUserId().equals(payload.userId())
                        && existing.getPaymentAttempt().equals(payload.paymentAttempt())
                        && existing.getAmount().compareTo(payload.amount()) == 0
                        && existing.getCurrency().equals(payload.currency())
                        && sameTimestamp(existing.getHoldExpiresAt(), payload.holdExpiresAt())
                        && sameTimestamp(existing.getRequestedAt(), payload.requestedAt())
                        && existing.getCorrelationId().equals(message.correlationId());

        if (!consistent) {
            throw new ConflictException(PaymentErrorCode.PAYMENT_ATTEMPT_PAYLOAD_MISMATCH);
        }
    }

    private static boolean sameTimestamp(OffsetDateTime first, OffsetDateTime second) {

        if (first == null || second == null) {
            return first == second;
        }

        return first.truncatedTo(ChronoUnit.MICROS).isEqual(second.truncatedTo(ChronoUnit.MICROS));
    }
}
