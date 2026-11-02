package com.cinema.payment.service.impl;

import com.cinema.common.api.mapper.PageResponseMapper;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.response.model.PageResponse;
import com.cinema.payment.controller.response.PaymentReconciliationCaseResponse;
import com.cinema.payment.entity.ReconciliationCase;
import com.cinema.payment.enums.ReconciliationStatus;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.mapper.PaymentManagementMapper;
import com.cinema.payment.repository.ReconciliationCaseRepository;
import com.cinema.payment.service.ReconciliationManagementReadService;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ReconciliationManagementReadServiceImpl
        implements ReconciliationManagementReadService {

    private final ReconciliationCaseRepository reconciliationCaseRepository;

    private final PaymentManagementMapper paymentManagementMapper;

    public ReconciliationManagementReadServiceImpl(
            ReconciliationCaseRepository reconciliationCaseRepository,
            PaymentManagementMapper paymentManagementMapper) {

        this.reconciliationCaseRepository = reconciliationCaseRepository;
        this.paymentManagementMapper = paymentManagementMapper;
    }

    @Override
    public PageResponse<PaymentReconciliationCaseResponse> findAll(
            ReconciliationStatus status, UUID paymentId, int page, int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw invalidQuery();
        }

        if (paymentId != null) {
            requireUuidV7(paymentId);
        }

        Specification<ReconciliationCase> specification =
                (root, query, builder) -> {
                    List<Predicate> predicates = new ArrayList<>();

                    if (status != null) {
                        predicates.add(builder.equal(root.get("status"), status));
                    }

                    if (paymentId != null) {
                        predicates.add(builder.equal(root.get("paymentId"), paymentId));
                    }

                    return builder.and(predicates.toArray(Predicate[]::new));
                };

        PageRequest pageRequest =
                PageRequest.of(
                        page, size, Sort.by(Sort.Order.asc("openedAt"), Sort.Order.asc("id")));

        Page<ReconciliationCase> cases =
                reconciliationCaseRepository.findAll(specification, pageRequest);

        return PageResponseMapper.map(cases.map(paymentManagementMapper::toResponse));
    }

    @Override
    public PaymentReconciliationCaseResponse findById(UUID reconciliationCaseId) {

        if (reconciliationCaseId == null) {
            throw new ValidationException(PaymentErrorCode.RECONCILIATION_CASE_ID_REQUIRED);
        }

        requireUuidV7(reconciliationCaseId);

        ReconciliationCase reconciliationCase =
                reconciliationCaseRepository
                        .findById(reconciliationCaseId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        PaymentErrorCode.RECONCILIATION_CASE_NOT_FOUND));

        return paymentManagementMapper.toResponse(reconciliationCase);
    }

    private static void requireUuidV7(UUID value) {

        if (value.version() != 7) {
            throw invalidQuery();
        }
    }

    private static ValidationException invalidQuery() {

        return new ValidationException(PaymentErrorCode.INVALID_MANAGEMENT_QUERY);
    }
}
