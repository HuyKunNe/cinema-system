package com.cinema.payment.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.entity.FinancialAuditRecord;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.entity.ReconciliationCase;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.enums.ReconciliationStatus;
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.model.ReconciliationOperationResult;
import com.cinema.payment.model.ReconciliationRejectRequest;
import com.cinema.payment.model.ReconciliationResolveRequest;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.repository.ReconciliationCaseRepository;
import com.cinema.payment.service.ReconciliationAdminService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ReconciliationAdminServiceImpl implements ReconciliationAdminService {

    private final ReconciliationCaseRepository reconciliationCaseRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final FinancialAuditRecordRepository financialAuditRecordRepository;
    private final Clock clock;

    public ReconciliationAdminServiceImpl(
            ReconciliationCaseRepository reconciliationCaseRepository,
            PaymentRepository paymentRepository,
            PaymentTransactionRepository transactionRepository,
            FinancialAuditRecordRepository financialAuditRecordRepository,
            Clock clock) {

        this.reconciliationCaseRepository = reconciliationCaseRepository;
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.financialAuditRecordRepository = financialAuditRecordRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ReconciliationOperationResult resolve(ReconciliationResolveRequest request) {

        validateResolveRequest(request);

        ReconciliationCase caseReference =
                reconciliationCaseRepository
                        .findById(request.reconciliationCaseId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                PaymentErrorCode.RECONCILIATION_CASE_NOT_FOUND));

        UUID paymentId = caseReference.getPaymentId();
        UUID transactionId = caseReference.getPaymentTransactionId();

        Payment payment =
                paymentRepository
                        .findByIdForUpdate(paymentId)
                        .orElseThrow(
                                () -> new NotFoundException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        PaymentTransaction transaction =
                transactionRepository
                        .findByIdForUpdate(transactionId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                PaymentErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));

        ReconciliationCase reconciliationCase =
                reconciliationCaseRepository
                        .findByIdForUpdate(request.reconciliationCaseId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                PaymentErrorCode.RECONCILIATION_CASE_NOT_FOUND));

        validateLockedAggregate(paymentId, transactionId, reconciliationCase, payment, transaction);

        OffsetDateTime resolvedAt = OffsetDateTime.now(clock);

        switch (request.resolution()) {
            case REFUND_SUCCEEDED ->
                    resolveRefundSuccess(payment, transaction, request, resolvedAt);

            case REFUND_FAILED -> resolveRefundFailure(payment, transaction, request, resolvedAt);
        }

        reconciliationCase.resolve(
                request.resolution(),
                request.actorType(),
                request.actorId(),
                request.reason(),
                resolvedAt);

        persistReconciliationAudit(
                payment,
                transaction,
                reconciliationCase,
                FinancialAuditAction.RECONCILIATION_RESOLVED,
                request.actorType(),
                request.actorId(),
                request.reason(),
                request.correlationId(),
                resolvedAt);

        return new ReconciliationOperationResult(
                reconciliationCase.getId(),
                payment.getId(),
                transaction.getId(),
                reconciliationCase.getStatus(),
                reconciliationCase.getResolution(),
                payment.getRefundStatus(),
                transaction.getStatus(),
                reconciliationCase.getResolvedAt());
    }

    @Override
    @Transactional
    public ReconciliationOperationResult reject(ReconciliationRejectRequest request) {

        validateRejectRequest(request);

        ReconciliationCase caseReference =
                reconciliationCaseRepository
                        .findById(request.reconciliationCaseId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                PaymentErrorCode.RECONCILIATION_CASE_NOT_FOUND));

        UUID paymentId = caseReference.getPaymentId();
        UUID transactionId = caseReference.getPaymentTransactionId();

        Payment payment =
                paymentRepository
                        .findByIdForUpdate(paymentId)
                        .orElseThrow(
                                () -> new NotFoundException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        PaymentTransaction transaction =
                transactionRepository
                        .findByIdForUpdate(transactionId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                PaymentErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));

        ReconciliationCase reconciliationCase =
                reconciliationCaseRepository
                        .findByIdForUpdate(request.reconciliationCaseId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                PaymentErrorCode.RECONCILIATION_CASE_NOT_FOUND));

        validateLockedAggregate(paymentId, transactionId, reconciliationCase, payment, transaction);

        OffsetDateTime rejectedAt = OffsetDateTime.now(clock);

        reconciliationCase.reject(
                request.actorType(), request.actorId(), request.reason(), rejectedAt);

        persistReconciliationAudit(
                payment,
                transaction,
                reconciliationCase,
                FinancialAuditAction.RECONCILIATION_REJECTED,
                request.actorType(),
                request.actorId(),
                request.reason(),
                request.correlationId(),
                rejectedAt);

        return new ReconciliationOperationResult(
                reconciliationCase.getId(),
                payment.getId(),
                transaction.getId(),
                reconciliationCase.getStatus(),
                reconciliationCase.getResolution(),
                payment.getRefundStatus(),
                transaction.getStatus(),
                reconciliationCase.getResolvedAt());
    }

    private static void resolveRefundSuccess(
            Payment payment,
            PaymentTransaction transaction,
            ReconciliationResolveRequest request,
            OffsetDateTime resolvedAt) {

        String providerReference =
                firstNonBlank(request.providerReference(), transaction.getProviderReference());

        if (providerReference == null) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REFERENCE_REQUIRED);
        }

        transaction.completeReconciledRefundSuccess(providerReference, resolvedAt);

        payment.completeRefundSuccess();
    }

    private static void resolveRefundFailure(
            Payment payment,
            PaymentTransaction transaction,
            ReconciliationResolveRequest request,
            OffsetDateTime resolvedAt) {

        if (request.failureCode() == null || request.failureCode().isBlank()) {

            throw new ValidationException(PaymentErrorCode.PROVIDER_FAILURE_CODE_REQUIRED);
        }

        transaction.completeReconciledRefundFailure(
                request.failureCode(), request.failureMessage(), resolvedAt);

        payment.completeRefundFailure();
    }

    private static void validateResolveRequest(ReconciliationResolveRequest request) {

        if (request == null) {
            throw new ValidationException(PaymentErrorCode.RECONCILIATION_REQUEST_REQUIRED);
        }

        if (request.reconciliationCaseId() == null) {
            throw new ValidationException(PaymentErrorCode.RECONCILIATION_CASE_ID_REQUIRED);
        }

        if (request.resolution() == null) {
            throw new ValidationException(PaymentErrorCode.RECONCILIATION_RESOLUTION_REQUIRED);
        }

        if (request.actorType() == null) {
            throw new ValidationException(PaymentErrorCode.RECONCILIATION_ACTOR_TYPE_REQUIRED);
        }

        if (request.actorId() == null || request.actorId().isBlank()) {

            throw new ValidationException(PaymentErrorCode.RECONCILIATION_ACTOR_ID_REQUIRED);
        }

        if (request.correlationId() == null) {
            throw new ValidationException(PaymentErrorCode.CORRELATION_ID_REQUIRED);
        }

        if (request.requestedAt() == null) {
            throw new ValidationException(PaymentErrorCode.REQUESTED_AT_REQUIRED);
        }
    }

    private static void validateAggregate(
            ReconciliationCase reconciliationCase,
            Payment payment,
            PaymentTransaction transaction) {

        if (reconciliationCase.getStatus() != ReconciliationStatus.OPEN) {

            throw new ConflictException(PaymentErrorCode.RECONCILIATION_CASE_NOT_OPEN);
        }

        if (!reconciliationCase.getPaymentId().equals(payment.getId())
                || !reconciliationCase.getPaymentTransactionId().equals(transaction.getId())
                || !transaction.getPaymentId().equals(payment.getId())
                || transaction.getTransactionType() != PaymentTransactionType.REFUND
                || !reconciliationCase.getProvider().equals(transaction.getProvider())) {

            throw new ConflictException(PaymentErrorCode.RECONCILIATION_DATA_MISMATCH);
        }

        if (payment.getStatus() != PaymentStatus.SUCCEEDED
                || payment.getRefundStatus() != RefundStatus.PENDING) {

            throw new ConflictException(PaymentErrorCode.REFUND_NOT_PENDING);
        }

        if (transaction.getStatus() != PaymentTransactionStatus.PENDING_PROVIDER) {

            throw new ConflictException(PaymentErrorCode.RECONCILIATION_TRANSACTION_NOT_PENDING);
        }
    }

    private static String firstNonBlank(String preferred, String fallback) {

        if (preferred != null && !preferred.isBlank()) {
            return preferred.strip();
        }

        if (fallback != null && !fallback.isBlank()) {
            return fallback.strip();
        }

        return null;
    }

    private static void validateRejectRequest(ReconciliationRejectRequest request) {

        if (request == null) {
            throw new ValidationException(PaymentErrorCode.RECONCILIATION_REQUEST_REQUIRED);
        }

        if (request.reconciliationCaseId() == null) {
            throw new ValidationException(PaymentErrorCode.RECONCILIATION_CASE_ID_REQUIRED);
        }

        if (request.actorType() == null) {
            throw new ValidationException(PaymentErrorCode.RECONCILIATION_ACTOR_TYPE_REQUIRED);
        }

        if (request.actorId() == null || request.actorId().isBlank()) {

            throw new ValidationException(PaymentErrorCode.RECONCILIATION_ACTOR_ID_REQUIRED);
        }

        if (request.correlationId() == null) {
            throw new ValidationException(PaymentErrorCode.CORRELATION_ID_REQUIRED);
        }

        if (request.requestedAt() == null) {
            throw new ValidationException(PaymentErrorCode.REQUESTED_AT_REQUIRED);
        }
    }

    private void persistReconciliationAudit(
            Payment payment,
            PaymentTransaction transaction,
            ReconciliationCase reconciliationCase,
            FinancialAuditAction action,
            FinancialAuditActorType actorType,
            String actorId,
            String reason,
            UUID correlationId,
            OffsetDateTime occurredAt) {

        String metadata =
                "reconciliationCaseId="
                        + reconciliationCase.getId()
                        + ",refundTransactionId="
                        + transaction.getId()
                        + ",provider="
                        + transaction.getProvider();

        FinancialAuditRecord auditRecord =
                new FinancialAuditRecord(
                        payment.getId(),
                        action,
                        actorType,
                        actorId,
                        reason,
                        metadata,
                        correlationId,
                        occurredAt);

        financialAuditRecordRepository.save(auditRecord);
    }

    private static void validateLockedAggregate(
            UUID expectedPaymentId,
            UUID expectedTransactionId,
            ReconciliationCase reconciliationCase,
            Payment payment,
            PaymentTransaction transaction) {

        if (!expectedPaymentId.equals(reconciliationCase.getPaymentId())
                || !expectedTransactionId.equals(reconciliationCase.getPaymentTransactionId())
                || !expectedPaymentId.equals(payment.getId())
                || !expectedPaymentId.equals(transaction.getPaymentId())
                || !expectedTransactionId.equals(transaction.getId())) {

            throw new ConflictException(PaymentErrorCode.RECONCILIATION_DATA_MISMATCH);
        }

        validateAggregate(reconciliationCase, payment, transaction);
    }
}
