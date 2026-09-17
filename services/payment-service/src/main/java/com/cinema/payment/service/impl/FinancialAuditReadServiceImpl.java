package com.cinema.payment.service.impl;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.controller.response.FinancialAuditRecordResponse;
import com.cinema.payment.entity.FinancialAuditRecord;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.service.FinancialAuditReadService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FinancialAuditReadServiceImpl implements FinancialAuditReadService {

    private final PaymentRepository paymentRepository;

    private final FinancialAuditRecordRepository financialAuditRecordRepository;

    public FinancialAuditReadServiceImpl(
            PaymentRepository paymentRepository,
            FinancialAuditRecordRepository financialAuditRecordRepository) {

        this.paymentRepository = paymentRepository;
        this.financialAuditRecordRepository = financialAuditRecordRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialAuditRecordResponse> findByPaymentId(UUID paymentId) {

        if (paymentId == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_ID_REQUIRED);
        }

        if (!paymentRepository.existsById(paymentId)) {
            throw new NotFoundException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }

        return financialAuditRecordRepository
                .findAllByPaymentIdOrderByOccurredAtAscIdAsc(paymentId)
                .stream()
                .map(FinancialAuditReadServiceImpl::toResponse)
                .toList();
    }

    private static FinancialAuditRecordResponse toResponse(FinancialAuditRecord record) {

        return new FinancialAuditRecordResponse(
                record.getId(),
                record.getPaymentId(),
                record.getAction(),
                record.getActorType(),
                record.getActorId(),
                record.getReason(),
                record.getMetadata(),
                record.getCorrelationId(),
                record.getOccurredAt());
    }
}
