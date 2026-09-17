package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.controller.response.FinancialAuditRecordResponse;
import com.cinema.payment.entity.FinancialAuditRecord;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class FinancialAuditReadServiceImplTest {

    private static final UUID PAYMENT_ID = UUID.fromString("019c1234-2222-7abc-8def-0123456789ab");

    private static final UUID CORRELATION_ID =
            UUID.fromString("019c1234-4444-7abc-8def-0123456789ab");

    @Mock private PaymentRepository paymentRepository;

    @Mock private FinancialAuditRecordRepository financialAuditRecordRepository;

    @Test
    void findByPaymentIdShouldReturnOrderedAuditRecords() {

        FinancialAuditRecord audit =
                new FinancialAuditRecord(
                        PAYMENT_ID,
                        FinancialAuditAction.REFUND_REQUESTED,
                        FinancialAuditActorType.USER,
                        "admin-123",
                        "Customer requested refund",
                        null,
                        CORRELATION_ID,
                        OffsetDateTime.parse("2026-09-17T03:30:00Z"));

        when(paymentRepository.existsById(PAYMENT_ID)).thenReturn(true);

        when(financialAuditRecordRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(PAYMENT_ID))
                .thenReturn(List.of(audit));

        FinancialAuditReadServiceImpl service =
                new FinancialAuditReadServiceImpl(
                        paymentRepository, financialAuditRecordRepository);

        List<FinancialAuditRecordResponse> result = service.findByPaymentId(PAYMENT_ID);

        assertThat(result).hasSize(1);

        FinancialAuditRecordResponse response = result.getFirst();

        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);

        assertThat(response.action()).isEqualTo(FinancialAuditAction.REFUND_REQUESTED);

        assertThat(response.actorType()).isEqualTo(FinancialAuditActorType.USER);

        assertThat(response.actorId()).isEqualTo("admin-123");

        assertThat(response.reason()).isEqualTo("Customer requested refund");

        assertThat(response.correlationId()).isEqualTo(CORRELATION_ID);

        verify(financialAuditRecordRepository)
                .findAllByPaymentIdOrderByOccurredAtAscIdAsc(PAYMENT_ID);
    }

    @Test
    void findByPaymentIdShouldReturnEmptyListWhenNoAuditExists() {

        when(paymentRepository.existsById(PAYMENT_ID)).thenReturn(true);

        when(financialAuditRecordRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(PAYMENT_ID))
                .thenReturn(List.of());

        FinancialAuditReadServiceImpl service =
                new FinancialAuditReadServiceImpl(
                        paymentRepository, financialAuditRecordRepository);

        List<FinancialAuditRecordResponse> result = service.findByPaymentId(PAYMENT_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void findByPaymentIdShouldRejectNullPaymentId() {

        FinancialAuditReadServiceImpl service =
                new FinancialAuditReadServiceImpl(
                        paymentRepository, financialAuditRecordRepository);

        assertThatThrownBy(() -> service.findByPaymentId(null))
                .isInstanceOf(ValidationException.class);

        verify(paymentRepository, never()).existsById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void findByPaymentIdShouldRejectMissingPayment() {

        when(paymentRepository.existsById(PAYMENT_ID)).thenReturn(false);

        FinancialAuditReadServiceImpl service =
                new FinancialAuditReadServiceImpl(
                        paymentRepository, financialAuditRecordRepository);

        assertThatThrownBy(() -> service.findByPaymentId(PAYMENT_ID))
                .isInstanceOf(NotFoundException.class);

        verify(financialAuditRecordRepository, never())
                .findAllByPaymentIdOrderByOccurredAtAscIdAsc(PAYMENT_ID);
    }
}
