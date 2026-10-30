package com.cinema.payment.service.impl;

import com.cinema.common.api.mapper.PageResponseMapper;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.response.model.PageResponse;
import com.cinema.payment.controller.response.PaymentManagementResponse;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.mapper.PaymentManagementMapper;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.service.PaymentManagementReadService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class PaymentManagementReadServiceImpl implements PaymentManagementReadService {

    private final PaymentRepository paymentRepository;

    private final PaymentManagementMapper paymentManagementMapper;

    public PaymentManagementReadServiceImpl(
            PaymentRepository paymentRepository, PaymentManagementMapper paymentManagementMapper) {

        this.paymentRepository = paymentRepository;
        this.paymentManagementMapper = paymentManagementMapper;
    }

    @Override
    public PageResponse<PaymentManagementResponse> findByBookingId(
            UUID bookingId, int page, int size) {

        if (bookingId == null) {
            throw new ValidationException(PaymentErrorCode.BOOKING_ID_REQUIRED);
        }

        requireUuidV7(bookingId);

        if (page < 0 || size < 1 || size > 100) {
            throw invalidQuery();
        }

        PageRequest pageRequest =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Order.desc("paymentAttempt"), Sort.Order.desc("id")));

        Page<Payment> payments = paymentRepository.findAllByBookingId(bookingId, pageRequest);

        return PageResponseMapper.map(payments.map(paymentManagementMapper::toResponse));
    }

    @Override
    public PaymentManagementResponse findById(UUID paymentId) {

        if (paymentId == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_ID_REQUIRED);
        }

        requireUuidV7(paymentId);

        Payment payment =
                paymentRepository
                        .findById(paymentId)
                        .orElseThrow(
                                () -> new NotFoundException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        return paymentManagementMapper.toResponse(payment);
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
