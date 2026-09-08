package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PaymentProviderOperationPreparationServiceImplTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-08T10:00:00Z");

    private static final String PROCESSING_OWNER = "payment-provider-operation:worker-1";

    @Mock private PaymentRepository paymentRepository;

    @Mock private PaymentTransactionRepository transactionRepository;

    private PaymentProviderOperationPreparationServiceImpl preparationService;

    @BeforeEach
    void setUp() {

        Clock clock = Clock.fixed(Instant.parse("2026-09-08T10:00:00Z"), ZoneOffset.UTC);

        preparationService =
                new PaymentProviderOperationPreparationServiceImpl(
                        paymentRepository, transactionRepository, clock);
    }

    @Test
    void shouldLockPaymentBeforeTransactionAndPrepareCharge() {

        Payment payment = payment(NOW.plusMinutes(5));

        PaymentTransaction transaction = claimedTransaction(payment);

        when(transactionRepository.findById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        when(transactionRepository.findByIdForUpdate(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        ClaimedProviderChargeOperation operation =
                preparationService.prepareCharge(transaction.getId(), PROCESSING_OWNER);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);

        assertThat(operation.transactionId()).isEqualTo(transaction.getId());

        assertThat(operation.processingOwner()).isEqualTo(PROCESSING_OWNER);

        assertThat(operation.provider()).isEqualTo("MOCK");

        assertThat(operation.idempotencyKey()).isEqualTo(transaction.getIdempotencyKey());

        assertThat(operation.command().paymentId()).isEqualTo(payment.getId());

        assertThat(operation.command().bookingId()).isEqualTo(payment.getBookingId());

        assertThat(operation.command().amount()).isEqualByComparingTo(payment.getAmount());

        assertThat(operation.command().currency()).isEqualTo(payment.getCurrency());

        assertThat(operation.command().holdExpiresAt()).isEqualTo(payment.getHoldExpiresAt());

        InOrder lockOrder = inOrder(transactionRepository, paymentRepository);

        lockOrder.verify(transactionRepository).findById(transaction.getId());

        lockOrder.verify(paymentRepository).findByIdForUpdate(payment.getId());

        lockOrder.verify(transactionRepository).findByIdForUpdate(transaction.getId());
    }

    @Test
    void expiredLeaseShouldRejectPreparation() {

        Payment payment = payment(NOW.plusMinutes(5));

        PaymentTransaction transaction = transaction(payment);

        transaction.claim(PROCESSING_OWNER, NOW.minusSeconds(30), NOW);

        stubLockedEntities(payment, transaction);

        assertThatThrownBy(
                        () ->
                                preparationService.prepareCharge(
                                        transaction.getId(), PROCESSING_OWNER))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .PAYMENT_TRANSACTION_LEASE_NOT_OWNED));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECEIVED);
    }

    @Test
    void expiredHoldShouldRejectPreparationBeforeProviderCall() {

        Payment payment = payment(NOW);

        PaymentTransaction transaction = claimedTransaction(payment);

        stubLockedEntities(payment, transaction);

        assertThatThrownBy(
                        () ->
                                preparationService.prepareCharge(
                                        transaction.getId(), PROCESSING_OWNER))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.PAYMENT_HOLD_EXPIRED));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECEIVED);
    }

    @Test
    void differentLeaseOwnerShouldRejectPreparation() {

        Payment payment = payment(NOW.plusMinutes(5));

        PaymentTransaction transaction = claimedTransaction(payment);

        stubLockedEntities(payment, transaction);

        assertThatThrownBy(
                        () ->
                                preparationService.prepareCharge(
                                        transaction.getId(), "another-worker"))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .PAYMENT_TRANSACTION_LEASE_NOT_OWNED));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECEIVED);
    }

    private void stubLockedEntities(Payment payment, PaymentTransaction transaction) {

        when(transactionRepository.findById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        when(transactionRepository.findByIdForUpdate(transaction.getId()))
                .thenReturn(Optional.of(transaction));
    }

    private static PaymentTransaction claimedTransaction(Payment payment) {

        PaymentTransaction transaction = transaction(payment);

        transaction.claim(PROCESSING_OWNER, NOW.minusSeconds(1), NOW.plusSeconds(30));

        return transaction;
    }

    private static PaymentTransaction transaction(Payment payment) {

        return new PaymentTransaction(
                payment.getId(),
                payment.getProvider(),
                PaymentTransactionType.CHARGE,
                1,
                payment.getAmount(),
                payment.getCurrency(),
                "charge:" + payment.getId(),
                payment.getRequestedAt());
    }

    private static Payment payment(OffsetDateTime holdExpiresAt) {

        return new Payment(
                UuidGenerator.next(),
                UuidGenerator.next(),
                1,
                new BigDecimal("125000.00"),
                "VND",
                "MOCK",
                holdExpiresAt,
                NOW.minusMinutes(1),
                UuidGenerator.next(),
                UuidGenerator.next());
    }
}
