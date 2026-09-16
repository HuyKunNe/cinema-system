package com.cinema.payment.service.impl;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.payment.config.PaymentProviderOperationProperties;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.service.RefundPaymentTransactionClaimService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RefundPaymentTransactionClaimServiceImpl
        implements RefundPaymentTransactionClaimService {

    private static final String PROCESSING_OWNER_PREFIX = "payment-refund-provider-operation:";

    private final PaymentTransactionRepository transactionRepository;

    private final PaymentProviderOperationProperties properties;

    private final Clock clock;

    public RefundPaymentTransactionClaimServiceImpl(
            PaymentTransactionRepository transactionRepository,
            PaymentProviderOperationProperties properties,
            Clock clock) {

        this.transactionRepository = transactionRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<PaymentTransaction> claimNextBatch() {

        OffsetDateTime claimedAt = OffsetDateTime.now(clock);

        int batchSize = properties.batchSize();

        List<PaymentTransaction> transactions = new ArrayList<>(batchSize);

        List<PaymentTransaction> expiredTransactions =
                transactionRepository.findExpiredProcessingRefundTransactions(claimedAt, batchSize);

        transactions.addAll(expiredTransactions);

        int remainingCapacity = batchSize - transactions.size();

        if (remainingCapacity > 0) {

            transactions.addAll(
                    transactionRepository.findReadyRefundTransactions(remainingCapacity));
        }

        if (transactions.isEmpty()) {
            return List.of();
        }

        String processingOwner = createProcessingOwner();

        OffsetDateTime processingExpiresAt = claimedAt.plus(properties.leaseDuration());

        transactions.forEach(
                transaction -> transaction.claim(processingOwner, claimedAt, processingExpiresAt));

        transactionRepository.saveAll(transactions);

        return List.copyOf(transactions);
    }

    private static String createProcessingOwner() {

        return PROCESSING_OWNER_PREFIX + UuidGenerator.next();
    }
}
