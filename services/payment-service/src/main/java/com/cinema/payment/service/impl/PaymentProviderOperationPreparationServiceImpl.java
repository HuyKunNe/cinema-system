package com.cinema.payment.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.provider.model.ProviderChargeCommand;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.service.PaymentProviderOperationPreparationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PaymentProviderOperationPreparationServiceImpl
        implements PaymentProviderOperationPreparationService {

    private static final int MAX_PROCESSING_OWNER_LENGTH = 150;

    private final PaymentRepository paymentRepository;

    private final PaymentTransactionRepository transactionRepository;

    private final Clock clock;

    public PaymentProviderOperationPreparationServiceImpl(
            PaymentRepository paymentRepository,
            PaymentTransactionRepository transactionRepository,
            Clock clock) {

        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ClaimedProviderChargeOperation prepareCharge(
            UUID transactionId, String processingOwner) {

        validateTransactionId(transactionId);

        String normalizedProcessingOwner = normalizeProcessingOwner(processingOwner);

        /*
         * This first read obtains the immutable payment ID without acquiring
         * the transaction-row lock. The lock order below remains:
         *
         * Payment -> PaymentTransaction
         */
        PaymentTransaction transactionReference =
                transactionRepository
                        .findById(transactionId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                PaymentErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));

        Payment payment =
                paymentRepository
                        .findByIdForUpdate(transactionReference.getPaymentId())
                        .orElseThrow(
                                () -> new NotFoundException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        PaymentTransaction transaction =
                transactionRepository
                        .findByIdForUpdate(transactionId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                PaymentErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));

        OffsetDateTime now = OffsetDateTime.now(clock);

        validateOwnership(transaction, normalizedProcessingOwner, now);

        validateChargeTransaction(transaction);
        validateConsistency(payment, transaction);

        if (!payment.getHoldExpiresAt().isAfter(now)) {
            throw new ConflictException(PaymentErrorCode.PAYMENT_HOLD_EXPIRED);
        }

        payment.startProcessing();

        ProviderChargeCommand command =
                new ProviderChargeCommand(
                        payment.getId(),
                        payment.getBookingId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getHoldExpiresAt());

        return new ClaimedProviderChargeOperation(
                transaction.getId(),
                normalizedProcessingOwner,
                transaction.getProvider(),
                transaction.getIdempotencyKey(),
                command);
    }

    private static void validateOwnership(
            PaymentTransaction transaction, String processingOwner, OffsetDateTime now) {

        if (!transaction.hasActiveLease(processingOwner, now)) {
            throw new ConflictException(PaymentErrorCode.PAYMENT_TRANSACTION_LEASE_NOT_OWNED);
        }
    }

    private static void validateChargeTransaction(PaymentTransaction transaction) {

        if (transaction.getTransactionType() != PaymentTransactionType.CHARGE) {

            throw new ConflictException(PaymentErrorCode.PAYMENT_TRANSACTION_TYPE_UNSUPPORTED);
        }
    }

    private static void validateConsistency(Payment payment, PaymentTransaction transaction) {

        boolean consistent =
                transaction.getPaymentId().equals(payment.getId())
                        && transaction.getProvider().equals(payment.getProvider())
                        && transaction.getAmount().compareTo(payment.getAmount()) == 0
                        && transaction.getCurrency().equals(payment.getCurrency());

        if (!consistent) {
            throw new InternalServerException(PaymentErrorCode.PAYMENT_TRANSACTION_DATA_MISMATCH);
        }
    }

    private static void validateTransactionId(UUID transactionId) {

        if (transactionId == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_TRANSACTION_ID_REQUIRED);
        }
    }

    private static String normalizeProcessingOwner(String processingOwner) {

        if (processingOwner == null || processingOwner.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROCESSING_OWNER_REQUIRED);
        }

        String normalizedProcessingOwner = processingOwner.strip();

        if (normalizedProcessingOwner.length() > MAX_PROCESSING_OWNER_LENGTH) {

            throw new ValidationException(PaymentErrorCode.PROCESSING_OWNER_INVALID);
        }

        return normalizedProcessingOwner;
    }
}
