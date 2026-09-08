package com.cinema.payment.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.AppliedProviderChargeResult;
import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.provider.model.ProviderChargeResult;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.service.PaymentProviderResultApplicationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class PaymentProviderResultApplicationServiceImpl
        implements PaymentProviderResultApplicationService {

    private final PaymentRepository paymentRepository;

    private final PaymentTransactionRepository transactionRepository;

    private final Clock clock;

    public PaymentProviderResultApplicationServiceImpl(
            PaymentRepository paymentRepository,
            PaymentTransactionRepository transactionRepository,
            Clock clock) {

        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AppliedProviderChargeResult apply(
            ClaimedProviderChargeOperation operation, ProviderChargeResult result) {

        if (operation == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_OPERATION_REQUIRED);
        }

        if (result == null || result.outcome() == null) {
            throw new InternalServerException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
        }

        PaymentTransaction transactionReference =
                transactionRepository
                        .findById(operation.transactionId())
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
                        .findByIdForUpdate(operation.transactionId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                PaymentErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));

        validateOwnership(operation, transaction);
        validateSnapshot(operation, payment, transaction);

        OffsetDateTime resultTime = OffsetDateTime.now(clock);

        applyOutcome(operation.processingOwner(), payment, transaction, result, resultTime);

        return new AppliedProviderChargeResult(
                payment.getId(), transaction.getId(), payment.getStatus(), transaction.getStatus());
    }

    private static void applyOutcome(
            String processingOwner,
            Payment payment,
            PaymentTransaction transaction,
            ProviderChargeResult result,
            OffsetDateTime resultTime) {

        switch (result.outcome()) {
            case SUCCEEDED -> {
                transaction.completeSuccessfully(
                        processingOwner, result.providerReference(), resultTime);

                payment.completeProviderSuccess(result.providerReference(), resultTime);
            }

            case FAILED -> {
                transaction.completeFailed(
                        processingOwner, result.failureCode(), result.failureMessage(), resultTime);

                payment.completeProviderFailure(
                        result.failureCode(), result.failureMessage(), resultTime);
            }

            case PENDING, UNKNOWN -> {
                transaction.markPendingProvider(
                        processingOwner,
                        result.providerReference(),
                        result.failureCode(),
                        result.failureMessage());

                payment.recordPendingProvider(result.providerReference(), resultTime);
            }
        }
    }

    private static void validateOwnership(
            ClaimedProviderChargeOperation operation, PaymentTransaction transaction) {

        /*
         * Do not check lease expiration here. A slow provider response remains
         * applicable while the processing owner has not been replaced.
         */
        if (!transaction.isOwnedBy(operation.processingOwner())) {
            throw new ConflictException(PaymentErrorCode.PAYMENT_TRANSACTION_LEASE_NOT_OWNED);
        }
    }

    private static void validateSnapshot(
            ClaimedProviderChargeOperation operation,
            Payment payment,
            PaymentTransaction transaction) {

        boolean consistent =
                operation.transactionId().equals(transaction.getId())
                        && operation.command().paymentId().equals(payment.getId())
                        && operation.command().bookingId().equals(payment.getBookingId())
                        && operation.provider().equals(payment.getProvider())
                        && operation.provider().equals(transaction.getProvider())
                        && operation.idempotencyKey().equals(transaction.getIdempotencyKey())
                        && operation.command().amount().compareTo(payment.getAmount()) == 0
                        && operation.command().amount().compareTo(transaction.getAmount()) == 0
                        && operation.command().currency().equals(payment.getCurrency())
                        && operation.command().currency().equals(transaction.getCurrency())
                        && operation.command().holdExpiresAt().equals(payment.getHoldExpiresAt());

        if (!consistent) {
            throw new InternalServerException(PaymentErrorCode.PAYMENT_TRANSACTION_DATA_MISMATCH);
        }
    }
}
