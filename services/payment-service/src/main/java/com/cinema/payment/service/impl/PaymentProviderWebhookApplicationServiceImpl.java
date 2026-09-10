package com.cinema.payment.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.webhook.model.PaymentProviderWebhookApplicationResult;
import com.cinema.payment.provider.webhook.model.ProviderWebhookApplicationDisposition;
import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.service.PaymentProviderWebhookApplicationService;
import com.cinema.payment.service.PaymentProviderWebhookEventRegistrationService;
import com.cinema.payment.service.PaymentResultOutboxService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class PaymentProviderWebhookApplicationServiceImpl
        implements PaymentProviderWebhookApplicationService {

    private final PaymentRepository paymentRepository;

    private final PaymentTransactionRepository transactionRepository;

    private final PaymentProviderWebhookEventRegistrationService eventRegistrationService;

    private final PaymentResultOutboxService paymentResultOutboxService;

    private final Clock clock;

    public PaymentProviderWebhookApplicationServiceImpl(
            PaymentRepository paymentRepository,
            PaymentTransactionRepository transactionRepository,
            PaymentProviderWebhookEventRegistrationService eventRegistrationService,
            PaymentResultOutboxService paymentResultOutboxService,
            Clock clock) {

        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.eventRegistrationService = eventRegistrationService;
        this.paymentResultOutboxService = paymentResultOutboxService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PaymentProviderWebhookApplicationResult apply(VerifiedProviderWebhook webhook) {

        if (webhook == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_REQUIRED);
        }

        PaymentTransaction transactionReference =
                transactionRepository
                        .findByProviderAndProviderReference(
                                webhook.provider(), webhook.providerReference())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                PaymentErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));

        /*
         * Required global lock order:
         * Payment -> PaymentTransaction.
         */
        Payment payment =
                paymentRepository
                        .findByIdForUpdate(transactionReference.getPaymentId())
                        .orElseThrow(
                                () -> new NotFoundException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        PaymentTransaction transaction =
                transactionRepository
                        .findByIdForUpdate(transactionReference.getId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                PaymentErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));

        validateTrustedData(payment, transactionReference, transaction, webhook);

        boolean registered = eventRegistrationService.register(transaction.getId(), webhook);

        if (!registered) {
            return result(payment, transaction, ProviderWebhookApplicationDisposition.DUPLICATE);
        }

        OffsetDateTime observedAt = OffsetDateTime.now(clock);

        ProviderWebhookApplicationDisposition disposition =
                applyOutcome(payment, transaction, webhook, observedAt);

        if (disposition == ProviderWebhookApplicationDisposition.APPLIED) {
            paymentResultOutboxService.persistIfTerminal(payment);
        }

        return result(payment, transaction, disposition);
    }

    private static ProviderWebhookApplicationDisposition applyOutcome(
            Payment payment,
            PaymentTransaction transaction,
            VerifiedProviderWebhook webhook,
            OffsetDateTime observedAt) {

        return switch (webhook.outcome()) {
            case SUCCEEDED -> applySuccess(payment, transaction, webhook, observedAt);

            case FAILED -> applyFailure(payment, transaction, webhook, observedAt);

            case PENDING, UNKNOWN -> applyPending(payment, transaction, webhook, observedAt);
        };
    }

    private static ProviderWebhookApplicationDisposition applySuccess(
            Payment payment,
            PaymentTransaction transaction,
            VerifiedProviderWebhook webhook,
            OffsetDateTime observedAt) {

        if (isConsistentExistingSuccess(payment, transaction, webhook)) {

            transaction.recordWebhookEvidence(webhook.providerEventId());

            return ProviderWebhookApplicationDisposition.CONFIRMED_EXISTING;
        }

        if (isTerminalOrReconciliation(payment, transaction)) {
            return ProviderWebhookApplicationDisposition.IGNORED_STALE;
        }

        requireApplicableStatePair(payment, transaction);

        transaction.completeSuccessfullyFromWebhook(
                webhook.providerEventId(), webhook.providerReference(), observedAt);

        payment.completeProviderSuccess(webhook.providerReference(), observedAt);

        return ProviderWebhookApplicationDisposition.APPLIED;
    }

    private static ProviderWebhookApplicationDisposition applyFailure(
            Payment payment,
            PaymentTransaction transaction,
            VerifiedProviderWebhook webhook,
            OffsetDateTime observedAt) {

        if (isConsistentExistingFailure(payment, transaction, webhook)) {

            transaction.recordWebhookEvidence(webhook.providerEventId());

            return ProviderWebhookApplicationDisposition.CONFIRMED_EXISTING;
        }

        if (isTerminalOrReconciliation(payment, transaction)) {
            return ProviderWebhookApplicationDisposition.IGNORED_STALE;
        }

        requireApplicableStatePair(payment, transaction);

        transaction.completeFailedFromWebhook(
                webhook.providerEventId(),
                webhook.providerReference(),
                webhook.failureCode(),
                webhook.failureMessage(),
                observedAt);

        payment.completeProviderFailure(
                webhook.failureCode(), webhook.failureMessage(), observedAt);

        return ProviderWebhookApplicationDisposition.APPLIED;
    }

    private static ProviderWebhookApplicationDisposition applyPending(
            Payment payment,
            PaymentTransaction transaction,
            VerifiedProviderWebhook webhook,
            OffsetDateTime observedAt) {

        if (isTerminalOrReconciliation(payment, transaction)) {
            return ProviderWebhookApplicationDisposition.IGNORED_STALE;
        }

        requireApplicableStatePair(payment, transaction);

        transaction.markPendingFromWebhook(
                webhook.providerEventId(),
                webhook.providerReference(),
                webhook.failureCode(),
                webhook.failureMessage());

        payment.recordPendingProvider(webhook.providerReference(), observedAt);

        return ProviderWebhookApplicationDisposition.APPLIED;
    }

    private static void validateTrustedData(
            Payment payment,
            PaymentTransaction transactionReference,
            PaymentTransaction transaction,
            VerifiedProviderWebhook webhook) {

        boolean consistent =
                transactionReference.getId().equals(transaction.getId())
                        && transactionReference.getPaymentId().equals(payment.getId())
                        && transaction.getPaymentId().equals(payment.getId())
                        && transaction.getTransactionType() == PaymentTransactionType.CHARGE
                        && webhook.provider().equals(payment.getProvider())
                        && webhook.provider().equals(transaction.getProvider())
                        && webhook.providerReference().equals(transaction.getProviderReference())
                        && webhook.amount().compareTo(payment.getAmount()) == 0
                        && webhook.amount().compareTo(transaction.getAmount()) == 0
                        && webhook.currency().equals(payment.getCurrency())
                        && webhook.currency().equals(transaction.getCurrency())
                        && paymentReferenceCompatible(payment, webhook.providerReference());

        if (!consistent) {
            throw new InternalServerException(PaymentErrorCode.PAYMENT_TRANSACTION_DATA_MISMATCH);
        }
    }

    private static boolean paymentReferenceCompatible(Payment payment, String providerReference) {

        return payment.getProviderReference() == null
                || payment.getProviderReference().equals(providerReference);
    }

    private static void requireApplicableStatePair(
            Payment payment, PaymentTransaction transaction) {

        boolean processingPair =
                payment.getStatus() == PaymentStatus.PROCESSING
                        && transaction.getStatus() == PaymentTransactionStatus.PROCESSING;

        boolean pendingPair =
                payment.getStatus() == PaymentStatus.PENDING_PROVIDER
                        && transaction.getStatus() == PaymentTransactionStatus.PENDING_PROVIDER;

        if (!processingPair && !pendingPair) {
            throw new ConflictException(PaymentErrorCode.PAYMENT_PROVIDER_RESULT_NOT_APPLICABLE);
        }
    }

    private static boolean isConsistentExistingSuccess(
            Payment payment, PaymentTransaction transaction, VerifiedProviderWebhook webhook) {

        boolean paymentSucceeded =
                payment.getStatus() == PaymentStatus.SUCCEEDED
                        || payment.getStatus() == PaymentStatus.RECONCILIATION_REQUIRED;

        return paymentSucceeded
                && transaction.getStatus() == PaymentTransactionStatus.SUCCEEDED
                && webhook.providerReference().equals(payment.getProviderReference())
                && webhook.providerReference().equals(transaction.getProviderReference());
    }

    private static boolean isConsistentExistingFailure(
            Payment payment, PaymentTransaction transaction, VerifiedProviderWebhook webhook) {

        return payment.getStatus() == PaymentStatus.FAILED
                && transaction.getStatus() == PaymentTransactionStatus.FAILED
                && webhook.failureCode().equals(payment.getFailureCode())
                && webhook.failureCode().equals(transaction.getFailureCode());
    }

    private static boolean isTerminalOrReconciliation(
            Payment payment, PaymentTransaction transaction) {

        return payment.isTerminal()
                || payment.getStatus() == PaymentStatus.RECONCILIATION_REQUIRED
                || transaction.getStatus() == PaymentTransactionStatus.SUCCEEDED
                || transaction.getStatus() == PaymentTransactionStatus.FAILED;
    }

    private static PaymentProviderWebhookApplicationResult result(
            Payment payment,
            PaymentTransaction transaction,
            ProviderWebhookApplicationDisposition disposition) {

        return new PaymentProviderWebhookApplicationResult(
                payment.getId(),
                transaction.getId(),
                payment.getStatus(),
                transaction.getStatus(),
                disposition);
    }
}
