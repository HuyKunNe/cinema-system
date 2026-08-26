package com.cinema.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
class PaymentRepositoryIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final UUID USER_ID = UuidGenerator.next();

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-08-26T10:00:00Z");

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @Autowired private EntityManager entityManager;

    @Test
    void shouldPersistPaymentAndChargeTransaction() {

        Payment payment = payment(UuidGenerator.next(), UuidGenerator.next(), 1);

        paymentRepository.saveAndFlush(payment);

        PaymentTransaction transaction = charge(payment.getId(), "charge:test:attempt-1");

        transactionRepository.saveAndFlush(transaction);

        entityManager.clear();

        Optional<Payment> savedPayment = paymentRepository.findById(payment.getId());

        Optional<PaymentTransaction> savedTransaction =
                transactionRepository.findById(transaction.getId());

        assertThat(savedPayment).isPresent();
        assertThat(savedPayment.orElseThrow().getStatus()).isEqualTo(PaymentStatus.RECEIVED);
        assertThat(savedPayment.orElseThrow().getCreatedAt()).isNotNull();
        assertThat(savedPayment.orElseThrow().getUpdatedAt()).isNotNull();

        assertThat(savedTransaction).isPresent();
        assertThat(savedTransaction.orElseThrow().getStatus())
                .isEqualTo(PaymentTransactionStatus.READY);
        assertThat(savedTransaction.orElseThrow().getCreatedAt()).isNotNull();
        assertThat(savedTransaction.orElseThrow().getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindPaymentByBookingAndAttempt() {

        UUID bookingId = UuidGenerator.next();

        Payment payment = payment(bookingId, UuidGenerator.next(), 2);

        paymentRepository.saveAndFlush(payment);

        entityManager.clear();

        Optional<Payment> result = paymentRepository.findByBookingIdAndPaymentAttempt(bookingId, 2);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo(payment.getId());
    }

    @Test
    void shouldFindPaymentBySourceEventId() {

        UUID sourceEventId = UuidGenerator.next();

        Payment payment = payment(UuidGenerator.next(), sourceEventId, 1);

        paymentRepository.saveAndFlush(payment);

        entityManager.clear();

        Optional<Payment> result = paymentRepository.findBySourceEventId(sourceEventId);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo(payment.getId());
    }

    @Test
    void shouldFindPaymentOnlyForItsOwner() {

        Payment payment = payment(UuidGenerator.next(), UuidGenerator.next(), 1);

        paymentRepository.saveAndFlush(payment);

        entityManager.clear();

        assertThat(paymentRepository.findByIdAndUserId(payment.getId(), USER_ID)).isPresent();

        assertThat(paymentRepository.findByIdAndUserId(payment.getId(), UuidGenerator.next()))
                .isEmpty();
    }

    @Test
    void shouldFindPaymentForUpdateInsideTransaction() {

        Payment payment = payment(UuidGenerator.next(), UuidGenerator.next(), 1);

        paymentRepository.saveAndFlush(payment);

        entityManager.clear();

        Optional<Payment> locked = paymentRepository.findByIdForUpdate(payment.getId());

        assertThat(locked).isPresent();
        assertThat(locked.orElseThrow().getId()).isEqualTo(payment.getId());
    }

    @Test
    void duplicateBookingAttemptShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        paymentRepository.saveAndFlush(payment(bookingId, UuidGenerator.next(), 1));

        Payment duplicate = payment(bookingId, UuidGenerator.next(), 1);

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateSourceEventShouldBeRejected() {

        UUID sourceEventId = UuidGenerator.next();

        paymentRepository.saveAndFlush(payment(UuidGenerator.next(), sourceEventId, 1));

        Payment duplicate = payment(UuidGenerator.next(), sourceEventId, 1);

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateProviderIdempotencyKeyShouldBeRejected() {

        Payment firstPayment = payment(UuidGenerator.next(), UuidGenerator.next(), 1);

        Payment secondPayment = payment(UuidGenerator.next(), UuidGenerator.next(), 1);

        paymentRepository.saveAllAndFlush(List.of(firstPayment, secondPayment));

        transactionRepository.saveAndFlush(charge(firstPayment.getId(), "charge:stable-key"));

        PaymentTransaction duplicate = charge(secondPayment.getId(), "charge:stable-key");

        assertThatThrownBy(() -> transactionRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void transactionForMissingPaymentShouldBeRejected() {

        PaymentTransaction transaction = charge(UuidGenerator.next(), "charge:missing-payment");

        assertThatThrownBy(() -> transactionRepository.saveAndFlush(transaction))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFindTransactionByProviderIdempotencyKey() {

        Payment payment = payment(UuidGenerator.next(), UuidGenerator.next(), 1);

        paymentRepository.saveAndFlush(payment);

        PaymentTransaction transaction = charge(payment.getId(), "charge:lookup-key");

        transactionRepository.saveAndFlush(transaction);

        entityManager.clear();

        Optional<PaymentTransaction> result =
                transactionRepository.findByProviderAndIdempotencyKey("MOCK", "charge:lookup-key");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo(transaction.getId());
    }

    private static Payment payment(UUID bookingId, UUID sourceEventId, int paymentAttempt) {

        return new Payment(
                bookingId,
                USER_ID,
                paymentAttempt,
                new BigDecimal("250000.00"),
                "VND",
                "MOCK",
                REQUESTED_AT.plusMinutes(10),
                REQUESTED_AT,
                sourceEventId,
                UuidGenerator.next());
    }

    private static PaymentTransaction charge(UUID paymentId, String idempotencyKey) {

        return new PaymentTransaction(
                paymentId,
                "MOCK",
                PaymentTransactionType.CHARGE,
                1,
                new BigDecimal("250000.00"),
                "VND",
                idempotencyKey,
                REQUESTED_AT);
    }
}
