package com.cinema.payment.service.impl;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.entity.FinancialAuditRecord;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.model.RefundRequest;
import com.cinema.payment.model.RefundRequestResult;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.service.RefundRequestService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundRequestServiceImpl implements RefundRequestService {

    private static final int MAX_REASON_LENGTH = 500;

    private static final String REFUND_IDEMPOTENCY_PREFIX = "refund:";

    private final PaymentRepository paymentRepository;

    private final PaymentTransactionRepository transactionRepository;

    private final FinancialAuditRecordRepository auditRepository;

    public RefundRequestServiceImpl(
            PaymentRepository paymentRepository,
            PaymentTransactionRepository transactionRepository,
            FinancialAuditRecordRepository auditRepository) {

        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    @Transactional
    public RefundRequestResult requestRefund(RefundRequest request) {

        validateRequest(request);

        Payment payment =
                paymentRepository
                        .findByIdForUpdate(request.paymentId())
                        .orElseThrow(
                                () -> new NotFoundException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        String idempotencyKey = refundIdempotencyKey(payment);

        PaymentTransaction existingTransaction =
                transactionRepository
                        .findByProviderAndIdempotencyKey(payment.getProvider(), idempotencyKey)
                        .orElse(null);

        if (existingTransaction != null) {

            validateExistingRefundTransaction(payment, existingTransaction);

            return new RefundRequestResult(
                    payment.getId(), existingTransaction.getId(), payment.getRefundStatus(), true);
        }

        payment.requestRefund();

        PaymentTransaction refundTransaction =
                new PaymentTransaction(
                        payment.getId(),
                        payment.getProvider(),
                        PaymentTransactionType.REFUND,
                        1,
                        payment.getAmount(),
                        payment.getCurrency(),
                        idempotencyKey,
                        request.requestedAt());

        transactionRepository.save(refundTransaction);

        FinancialAuditRecord auditRecord =
                new FinancialAuditRecord(
                        payment.getId(),
                        FinancialAuditAction.REFUND_REQUESTED,
                        request.actorType(),
                        request.actorId(),
                        normalizeReason(request.reason()),
                        null,
                        request.correlationId(),
                        request.requestedAt());

        auditRepository.save(auditRecord);

        /*
         * The Payment row is already managed by the persistence context.
         * Explicit save keeps the transaction boundary obvious and makes
         * the three atomic writes visible in this application service:
         *
         * Payment refund status
         * REFUND PaymentTransaction
         * REFUND_REQUESTED financial audit record
         */
        paymentRepository.save(payment);

        return new RefundRequestResult(
                payment.getId(), refundTransaction.getId(), payment.getRefundStatus(), false);
    }

    private static void validateRequest(RefundRequest request) {

        if (request == null) {
            throw new ValidationException(PaymentErrorCode.REFUND_REQUEST_REQUIRED);
        }

        if (request.paymentId() == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_ID_REQUIRED);
        }

        if (request.actorType() == null) {
            throw new ValidationException(PaymentErrorCode.FINANCIAL_AUDIT_ACTOR_TYPE_REQUIRED);
        }

        if (request.actorId() == null || request.actorId().isBlank()) {

            throw new ValidationException(PaymentErrorCode.FINANCIAL_AUDIT_ACTOR_ID_REQUIRED);
        }

        if (request.actorId().strip().length() > 255) {
            throw new ValidationException(PaymentErrorCode.FINANCIAL_AUDIT_ACTOR_ID_INVALID);
        }

        if (request.correlationId() == null) {
            throw new ValidationException(PaymentErrorCode.CORRELATION_ID_REQUIRED);
        }

        if (request.requestedAt() == null) {
            throw new ValidationException(PaymentErrorCode.REFUND_REQUESTED_AT_REQUIRED);
        }

        if (request.reason() != null && request.reason().strip().length() > MAX_REASON_LENGTH) {

            throw new ValidationException(PaymentErrorCode.REFUND_REASON_INVALID);
        }
    }

    private static String refundIdempotencyKey(Payment payment) {

        return REFUND_IDEMPOTENCY_PREFIX + payment.getId();
    }

    private static String normalizeReason(String reason) {

        if (reason == null || reason.isBlank()) {
            return null;
        }

        return reason.strip();
    }

    private static void validateExistingRefundTransaction(
            Payment payment, PaymentTransaction transaction) {

        boolean consistent =
                transaction.getPaymentId().equals(payment.getId())
                        && transaction.getTransactionType() == PaymentTransactionType.REFUND
                        && transaction.getProvider().equals(payment.getProvider())
                        && transaction.getAmount().compareTo(payment.getAmount()) == 0
                        && transaction.getCurrency().equals(payment.getCurrency());

        if (!consistent) {
            throw new InternalServerException(PaymentErrorCode.REFUND_TRANSACTION_DATA_MISMATCH);
        }
    }
}
