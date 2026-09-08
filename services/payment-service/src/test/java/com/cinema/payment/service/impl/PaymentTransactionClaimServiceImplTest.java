package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.payment.config.PaymentProviderOperationProperties;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.repository.PaymentTransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionClaimServiceImplTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-08T10:00:00Z");

    private static final Duration LEASE_DURATION = Duration.ofSeconds(30);

    @Mock private PaymentTransactionRepository transactionRepository;

    private PaymentTransactionClaimServiceImpl claimService;

    @BeforeEach
    void setUp() {

        PaymentProviderOperationProperties properties =
                new PaymentProviderOperationProperties(2, LEASE_DURATION);

        Clock clock = Clock.fixed(Instant.parse("2026-09-08T10:00:00Z"), ZoneOffset.UTC);

        claimService =
                new PaymentTransactionClaimServiceImpl(transactionRepository, properties, clock);
    }

    @Test
    void shouldClaimBoundedBatchUsingOneLeaseOwner() {

        PaymentTransaction firstTransaction = transaction("charge:first");

        PaymentTransaction secondTransaction = transaction("charge:second");

        List<PaymentTransaction> claimableTransactions =
                List.of(firstTransaction, secondTransaction);

        when(transactionRepository.findExpiredProcessingTransactions(NOW, 2)).thenReturn(List.of());

        when(transactionRepository.findReadyTransactions(2)).thenReturn(claimableTransactions);

        List<PaymentTransaction> result = claimService.claimNextBatch();

        assertThat(result).containsExactly(firstTransaction, secondTransaction);

        assertThat(firstTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.PROCESSING);

        assertThat(secondTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.PROCESSING);

        String processingOwner = firstTransaction.getProcessingOwner();

        assertThat(processingOwner).startsWith("payment-provider-operation:");

        assertThat(secondTransaction.getProcessingOwner()).isEqualTo(processingOwner);

        assertThat(firstTransaction.getProcessingExpiresAt()).isEqualTo(NOW.plus(LEASE_DURATION));

        assertThat(secondTransaction.getProcessingExpiresAt()).isEqualTo(NOW.plus(LEASE_DURATION));

        verify(transactionRepository).saveAll(claimableTransactions);
    }

    @Test
    void emptyBatchShouldNotInvokeSave() {

        when(transactionRepository.findExpiredProcessingTransactions(NOW, 2)).thenReturn(List.of());

        when(transactionRepository.findReadyTransactions(2)).thenReturn(List.of());

        List<PaymentTransaction> result = claimService.claimNextBatch();

        assertThat(result).isEmpty();

        verify(transactionRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void expiredProcessingTransactionsShouldUseCapacityBeforeReadyTransactions() {

        PaymentTransaction expiredTransaction = transaction("charge:expired");

        expiredTransaction.claim("expired-worker", NOW.minusMinutes(1), NOW.minusSeconds(1));

        PaymentTransaction readyTransaction = transaction("charge:ready");

        when(transactionRepository.findExpiredProcessingTransactions(NOW, 2))
                .thenReturn(List.of(expiredTransaction));

        when(transactionRepository.findReadyTransactions(1)).thenReturn(List.of(readyTransaction));

        List<PaymentTransaction> result = claimService.claimNextBatch();

        assertThat(result).containsExactly(expiredTransaction, readyTransaction);

        assertThat(expiredTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.PROCESSING);

        assertThat(readyTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.PROCESSING);

        assertThat(expiredTransaction.getProcessingOwner())
                .isEqualTo(readyTransaction.getProcessingOwner());

        verify(transactionRepository).findExpiredProcessingTransactions(NOW, 2);

        verify(transactionRepository).findReadyTransactions(1);

        verify(transactionRepository).saveAll(List.of(expiredTransaction, readyTransaction));
    }

    private static PaymentTransaction transaction(String idempotencyKey) {

        return new PaymentTransaction(
                UuidGenerator.next(),
                "MOCK",
                PaymentTransactionType.CHARGE,
                1,
                new BigDecimal("250000.00"),
                "VND",
                idempotencyKey,
                NOW.minusMinutes(1));
    }
}
