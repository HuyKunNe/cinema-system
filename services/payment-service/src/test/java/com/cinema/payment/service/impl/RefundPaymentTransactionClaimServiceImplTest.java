package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.payment.config.PaymentProviderOperationProperties;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.repository.PaymentTransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RefundPaymentTransactionClaimServiceImplTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-16T10:00:00Z");

    @Mock private PaymentTransactionRepository transactionRepository;

    @Mock private PaymentProviderOperationProperties properties;

    @Mock private PaymentTransaction expiredTransaction;

    @Mock private PaymentTransaction readyTransaction;

    private RefundPaymentTransactionClaimServiceImpl service;

    @BeforeEach
    void setUp() {

        Clock clock = Clock.fixed(Instant.parse("2026-09-16T10:00:00Z"), ZoneOffset.UTC);

        service =
                new RefundPaymentTransactionClaimServiceImpl(
                        transactionRepository, properties, clock);
    }

    @Test
    void shouldClaimExpiredRefundTransactionsBeforeReadyRefundTransactions() {

        when(properties.batchSize()).thenReturn(2);

        when(properties.leaseDuration()).thenReturn(Duration.ofSeconds(30));

        when(transactionRepository.findExpiredProcessingRefundTransactions(NOW, 2))
                .thenReturn(List.of(expiredTransaction));

        when(transactionRepository.findReadyRefundTransactions(1))
                .thenReturn(List.of(readyTransaction));

        List<PaymentTransaction> claimed = service.claimNextBatch();

        assertThat(claimed).containsExactly(expiredTransaction, readyTransaction);

        verify(expiredTransaction)
                .claim(
                        any(String.class),
                        org.mockito.ArgumentMatchers.eq(NOW),
                        org.mockito.ArgumentMatchers.eq(NOW.plusSeconds(30)));

        verify(readyTransaction)
                .claim(
                        any(String.class),
                        org.mockito.ArgumentMatchers.eq(NOW),
                        org.mockito.ArgumentMatchers.eq(NOW.plusSeconds(30)));

        verify(transactionRepository).saveAll(List.of(expiredTransaction, readyTransaction));

        InOrder order = inOrder(transactionRepository);

        order.verify(transactionRepository).findExpiredProcessingRefundTransactions(NOW, 2);

        order.verify(transactionRepository).findReadyRefundTransactions(1);

        order.verify(transactionRepository).saveAll(List.of(expiredTransaction, readyTransaction));
    }

    @Test
    void expiredRefundTransactionsShouldConsumeBatchCapacityBeforeReadyRefunds() {

        PaymentTransaction secondExpiredTransaction =
                org.mockito.Mockito.mock(PaymentTransaction.class);

        when(properties.batchSize()).thenReturn(2);

        when(properties.leaseDuration()).thenReturn(Duration.ofMinutes(1));

        when(transactionRepository.findExpiredProcessingRefundTransactions(NOW, 2))
                .thenReturn(List.of(expiredTransaction, secondExpiredTransaction));

        List<PaymentTransaction> claimed = service.claimNextBatch();

        assertThat(claimed).containsExactly(expiredTransaction, secondExpiredTransaction);

        verify(transactionRepository, never())
                .findReadyRefundTransactions(org.mockito.ArgumentMatchers.anyInt());

        verify(expiredTransaction)
                .claim(
                        any(String.class),
                        org.mockito.ArgumentMatchers.eq(NOW),
                        org.mockito.ArgumentMatchers.eq(NOW.plusMinutes(1)));

        verify(secondExpiredTransaction)
                .claim(
                        any(String.class),
                        org.mockito.ArgumentMatchers.eq(NOW),
                        org.mockito.ArgumentMatchers.eq(NOW.plusMinutes(1)));

        verify(transactionRepository)
                .saveAll(List.of(expiredTransaction, secondExpiredTransaction));
    }

    @Test
    void shouldReturnEmptyResultWithoutSavingWhenNoRefundTransactionsAreClaimable() {

        when(properties.batchSize()).thenReturn(10);

        when(transactionRepository.findExpiredProcessingRefundTransactions(NOW, 10))
                .thenReturn(List.of());

        when(transactionRepository.findReadyRefundTransactions(10)).thenReturn(List.of());

        List<PaymentTransaction> claimed = service.claimNextBatch();

        assertThat(claimed).isEmpty();

        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void claimedRefundTransactionsShouldUseRefundSpecificProcessingOwner() {

        when(properties.batchSize()).thenReturn(1);

        when(properties.leaseDuration()).thenReturn(Duration.ofSeconds(45));

        when(transactionRepository.findExpiredProcessingRefundTransactions(NOW, 1))
                .thenReturn(List.of());

        when(transactionRepository.findReadyRefundTransactions(1))
                .thenReturn(List.of(readyTransaction));

        service.claimNextBatch();

        org.mockito.ArgumentCaptor<String> ownerCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);

        verify(readyTransaction)
                .claim(
                        ownerCaptor.capture(),
                        org.mockito.ArgumentMatchers.eq(NOW),
                        org.mockito.ArgumentMatchers.eq(NOW.plusSeconds(45)));

        assertThat(ownerCaptor.getValue()).startsWith("payment-refund-provider-operation:");
    }
}
