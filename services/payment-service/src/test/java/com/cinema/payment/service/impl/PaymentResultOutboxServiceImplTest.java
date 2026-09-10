package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.event.PaymentFailedOutboxFactory;
import com.cinema.payment.event.PaymentSucceededOutboxFactory;
import com.cinema.payment.exception.PaymentErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentResultOutboxServiceImplTest {

    @Mock private PaymentSucceededOutboxFactory succeededOutboxFactory;

    @Mock private PaymentFailedOutboxFactory failedOutboxFactory;

    @Mock private OutboxRepository outboxRepository;

    @Mock private Payment payment;

    @Mock private OutboxEventEntity outboxEvent;

    private PaymentResultOutboxServiceImpl service;

    @BeforeEach
    void setUp() {

        service =
                new PaymentResultOutboxServiceImpl(
                        succeededOutboxFactory, failedOutboxFactory, outboxRepository);
    }

    @Test
    void succeededPaymentShouldPersistSucceededOutbox() {

        when(payment.getStatus()).thenReturn(PaymentStatus.SUCCEEDED);

        when(succeededOutboxFactory.create(payment)).thenReturn(outboxEvent);

        service.persistIfTerminal(payment);

        verify(succeededOutboxFactory).create(payment);

        verify(failedOutboxFactory, never()).create(payment);

        verify(outboxRepository).save(outboxEvent);
    }

    @Test
    void failedPaymentShouldPersistFailedOutbox() {

        when(payment.getStatus()).thenReturn(PaymentStatus.FAILED);

        when(failedOutboxFactory.create(payment)).thenReturn(outboxEvent);

        service.persistIfTerminal(payment);

        verify(failedOutboxFactory).create(payment);

        verify(succeededOutboxFactory, never()).create(payment);

        verify(outboxRepository).save(outboxEvent);
    }

    @Test
    void expiredPaymentShouldPersistFailedOutbox() {

        when(payment.getStatus()).thenReturn(PaymentStatus.EXPIRED);

        when(failedOutboxFactory.create(payment)).thenReturn(outboxEvent);

        service.persistIfTerminal(payment);

        verify(failedOutboxFactory).create(payment);

        verify(succeededOutboxFactory, never()).create(payment);

        verify(outboxRepository).save(outboxEvent);
    }

    @Test
    void pendingPaymentShouldNotPersistOutbox() {

        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING_PROVIDER);

        service.persistIfTerminal(payment);

        verify(succeededOutboxFactory, never()).create(payment);

        verify(failedOutboxFactory, never()).create(payment);

        verify(outboxRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reconciliationPaymentShouldNotPersistOutbox() {

        when(payment.getStatus()).thenReturn(PaymentStatus.RECONCILIATION_REQUIRED);

        service.persistIfTerminal(payment);

        verify(succeededOutboxFactory, never()).create(payment);

        verify(failedOutboxFactory, never()).create(payment);

        verify(outboxRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nullPaymentShouldUseCommonValidationException() {

        assertThatThrownBy(() -> service.persistIfTerminal(null))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        exception ->
                                org.assertj.core.api.Assertions.assertThat(
                                                ((ValidationException) exception).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.PAYMENT_REQUIRED));
    }
}
