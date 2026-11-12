package com.cinema.payment.service.impl;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.controller.response.PaymentCheckoutResponse;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.service.CustomerPaymentCheckoutReadService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CustomerPaymentCheckoutReadServiceImpl implements CustomerPaymentCheckoutReadService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final Clock clock;

    public CustomerPaymentCheckoutReadServiceImpl(
            PaymentRepository paymentRepository,
            PaymentTransactionRepository transactionRepository,
            Clock clock) {
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Override
    public PaymentCheckoutResponse findByBookingId(UUID userId, UUID bookingId) {
        validateIdentifier(userId, PaymentErrorCode.USER_ID_REQUIRED);
        validateIdentifier(bookingId, PaymentErrorCode.BOOKING_ID_REQUIRED);
        requireUuidV7(bookingId);

        Payment payment =
                paymentRepository
                        .findFirstByBookingIdAndUserIdOrderByPaymentAttemptDescIdDesc(
                                bookingId, userId)
                        .orElseThrow(
                                () -> new NotFoundException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        String checkoutUri = findActiveCheckoutUri(payment);

        return new PaymentCheckoutResponse(
                payment.getBookingId(),
                payment.getId(),
                payment.getStatus(),
                checkoutUri,
                payment.getHoldExpiresAt());
    }

    private String findActiveCheckoutUri(Payment payment) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (payment.getStatus() != PaymentStatus.PENDING_PROVIDER
                || !payment.getHoldExpiresAt().isAfter(now)) {
            return null;
        }

        return transactionRepository
                .findFirstByPaymentIdAndTransactionTypeOrderByAttemptNumberDescIdDesc(
                        payment.getId(), PaymentTransactionType.CHARGE)
                .filter(transaction ->
                        transaction.getStatus() == PaymentTransactionStatus.PENDING_PROVIDER)
                .map(PaymentTransaction::getCheckoutUri)
                .orElse(null);
    }

    private static void validateIdentifier(UUID value, PaymentErrorCode errorCode) {
        if (value == null) {
            throw new ValidationException(errorCode);
        }
    }

    private static void requireUuidV7(UUID value) {
        if (value.version() != 7) {
            throw new ValidationException(PaymentErrorCode.INVALID_MANAGEMENT_QUERY);
        }
    }
}
