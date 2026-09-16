package com.cinema.payment.service.impl;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.entity.FinancialAuditRecord;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.entity.ReconciliationCase;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.enums.ReconciliationReason;
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.AppliedProviderRefundResult;
import com.cinema.payment.provider.model.ClaimedProviderRefundOperation;
import com.cinema.payment.provider.model.ProviderRefundResult;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.repository.ReconciliationCaseRepository;
import com.cinema.payment.service.RefundProviderResultApplicationService;

@Service
public class RefundProviderResultApplicationServiceImpl
        implements RefundProviderResultApplicationService {

    private final PaymentRepository paymentRepository;

    private final PaymentTransactionRepository transactionRepository;

    private final Clock clock;

    private final FinancialAuditRecordRepository auditRepository;

    private final ReconciliationCaseRepository reconciliationCaseRepository;

    private static final String REFUND_PROVIDER_ACTOR_ID = "payment-refund-provider-operation";

    private static final String REFUND_SUCCEEDED_REASON =
            "Refund completed successfully by payment provider";

    private static final String REFUND_FAILED_REASON = "Refund failed at payment provider";

    public RefundProviderResultApplicationServiceImpl(
            PaymentRepository paymentRepository,
            PaymentTransactionRepository transactionRepository,
            FinancialAuditRecordRepository auditRepository,
            ReconciliationCaseRepository reconciliationCaseRepository,
            Clock clock) {

        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.auditRepository = auditRepository;
        this.reconciliationCaseRepository = reconciliationCaseRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AppliedProviderRefundResult apply(
            ClaimedProviderRefundOperation operation, ProviderRefundResult result) {

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

        persistFinancialOutcome(payment, transaction, result, resultTime);

        return new AppliedProviderRefundResult(
                payment.getId(),
                transaction.getId(),
                payment.getRefundStatus(),
                transaction.getStatus());
    }

    private static void applyOutcome(
            String processingOwner,
            Payment payment,
            PaymentTransaction transaction,
            ProviderRefundResult result,
            OffsetDateTime resultTime) {

        switch (result.outcome()) {
            case SUCCEEDED -> {
                transaction.completeSuccessfully(
                        processingOwner, result.providerReference(), resultTime);

                payment.completeRefundSuccess();
            }

            case FAILED -> {
                transaction.completeFailed(
                        processingOwner, result.failureCode(), result.failureMessage(), resultTime);

                payment.completeRefundFailure();
            }

            case PENDING, UNKNOWN -> {
                transaction.markPendingProvider(
                        processingOwner,
                        result.providerReference(),
                        result.failureCode(),
                        result.failureMessage());

                payment.recordPendingRefund();
            }
        }
    }

    private void persistFinancialOutcome(
            Payment payment,
            PaymentTransaction transaction,
            ProviderRefundResult result,
            OffsetDateTime occurredAt) {

        switch (result.outcome()) {
            case SUCCEEDED ->
                    persistTerminalAudit(
                            payment,
                            transaction,
                            FinancialAuditAction.REFUND_SUCCEEDED,
                            REFUND_SUCCEEDED_REASON,
                            occurredAt);

            case FAILED ->
                    persistTerminalAudit(
                            payment,
                            transaction,
                            FinancialAuditAction.REFUND_FAILED,
                            REFUND_FAILED_REASON,
                            occurredAt);

            case PENDING ->
                    openReconciliationCase(
                            payment,
                            transaction,
                            ReconciliationReason.REFUND_PROVIDER_PENDING,
                            result.providerReference(),
                            occurredAt);

            case UNKNOWN ->
                    openReconciliationCase(
                            payment,
                            transaction,
                            ReconciliationReason.REFUND_PROVIDER_UNKNOWN,
                            result.providerReference(),
                            occurredAt);
        }
    }

    private void persistTerminalAudit(
            Payment payment,
            PaymentTransaction transaction,
            FinancialAuditAction action,
            String reason,
            OffsetDateTime occurredAt) {

        FinancialAuditRecord auditRecord =
                new FinancialAuditRecord(
                        payment.getId(),
                        action,
                        FinancialAuditActorType.SYSTEM,
                        REFUND_PROVIDER_ACTOR_ID,
                        reason,
                        refundAuditMetadata(transaction),
                        UuidGenerator.next(),
                        occurredAt);

        auditRepository.save(auditRecord);
    }

    private void openReconciliationCase(
            Payment payment,
            PaymentTransaction transaction,
            ReconciliationReason reason,
            String providerReference,
            OffsetDateTime openedAt) {

        if (reconciliationCaseRepository
                .findByPaymentTransactionId(transaction.getId())
                .isPresent()) {

            return;
        }

        ReconciliationCase reconciliationCase =
                new ReconciliationCase(
                        payment.getId(),
                        transaction.getId(),
                        reason,
                        transaction.getProvider(),
                        providerReference,
                        openedAt);

        reconciliationCaseRepository.save(reconciliationCase);

        FinancialAuditRecord auditRecord =
                new FinancialAuditRecord(
                        payment.getId(),
                        FinancialAuditAction.RECONCILIATION_OPENED,
                        FinancialAuditActorType.SYSTEM,
                        REFUND_PROVIDER_ACTOR_ID,
                        reconciliationReason(reason),
                        reconciliationAuditMetadata(transaction, reconciliationCase),
                        UuidGenerator.next(),
                        openedAt);

        auditRepository.save(auditRecord);
    }

    private static String reconciliationReason(ReconciliationReason reason) {

        return switch (reason) {
            case REFUND_PROVIDER_PENDING -> "Refund provider returned a pending result";

            case REFUND_PROVIDER_UNKNOWN -> "Refund provider outcome requires reconciliation";
        };
    }

    private static String reconciliationAuditMetadata(
            PaymentTransaction transaction, ReconciliationCase reconciliationCase) {

        return "refundTransactionId="
                + transaction.getId()
                + ",reconciliationCaseId="
                + reconciliationCase.getId()
                + ",provider="
                + transaction.getProvider();
    }

    private static String refundAuditMetadata(PaymentTransaction transaction) {

        return "refundTransactionId="
                + transaction.getId()
                + ",provider="
                + transaction.getProvider();
    }

    private static void validateOwnership(
            ClaimedProviderRefundOperation operation, PaymentTransaction transaction) {

        /*
         * Do not reject a slow provider response only because its original
         * lease time elapsed. It remains applicable while another worker
         * has not replaced the processing owner.
         */
        if (!transaction.isOwnedBy(operation.processingOwner())) {

            throw new ConflictException(PaymentErrorCode.PAYMENT_TRANSACTION_LEASE_NOT_OWNED);
        }
    }

    private static void validateSnapshot(
            ClaimedProviderRefundOperation operation,
            Payment payment,
            PaymentTransaction transaction) {

        boolean consistent =
                operation.transactionId().equals(transaction.getId())
                        && operation.command().paymentId().equals(payment.getId())
                        && transaction.getPaymentId().equals(payment.getId())
                        && transaction.getTransactionType() == PaymentTransactionType.REFUND
                        && operation.provider().equals(payment.getProvider())
                        && operation.provider().equals(transaction.getProvider())
                        && operation.idempotencyKey().equals(transaction.getIdempotencyKey())
                        && operation
                                .command()
                                .chargeProviderReference()
                                .equals(payment.getProviderReference())
                        && operation.command().amount().compareTo(payment.getAmount()) == 0
                        && operation.command().amount().compareTo(transaction.getAmount()) == 0
                        && operation.command().currency().equals(payment.getCurrency())
                        && operation.command().currency().equals(transaction.getCurrency());

        if (!consistent) {

            throw new InternalServerException(PaymentErrorCode.REFUND_TRANSACTION_DATA_MISMATCH);
        }

        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {

            throw new ConflictException(PaymentErrorCode.PAYMENT_NOT_REFUNDABLE);
        }

        if (payment.getRefundStatus() != RefundStatus.PENDING) {

            throw new ConflictException(PaymentErrorCode.REFUND_NOT_PENDING);
        }
    }
}
