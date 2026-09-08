package com.cinema.payment.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.provider.model.ProviderChargeResult;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.service.PaymentProviderOperationPreparationService;
import com.cinema.payment.service.PaymentTransactionClaimService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Import(PaymentProviderOperationMySqlIntegrationTest.FixedClockConfiguration.class)
@TestPropertySource(
        properties = {
            "cinema.payment.provider=MOCK",
            "cinema.payment.provider-operation.batch-size=2",
            "cinema.payment.provider-operation.lease-duration=30s",
            "cinema.payment.kafka.enabled=false"
        })
class PaymentProviderOperationMySqlIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-08T10:00:00Z");

    private static final Duration LEASE_DURATION = Duration.ofSeconds(30);

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @Autowired private PaymentTransactionClaimService claimService;

    @Autowired private PaymentProviderOperationPreparationService preparationService;

    @Autowired private PaymentProviderOperationExecutor operationExecutor;

    @Autowired private PlatformTransactionManager transactionManager;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {

        transactionRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
    }

    @Test
    void claimShouldRespectConfiguredBatchSize() {

        persistReadyCharge("charge:batch:1");
        persistReadyCharge("charge:batch:2");
        persistReadyCharge("charge:batch:3");

        List<PaymentTransaction> claimedTransactions = claimService.claimNextBatch();

        assertThat(claimedTransactions).hasSize(2);

        assertThat(claimedTransactions)
                .allSatisfy(
                        transaction -> {
                            assertThat(transaction.getStatus())
                                    .isEqualTo(PaymentTransactionStatus.PROCESSING);

                            assertThat(transaction.getProcessingOwner())
                                    .startsWith("payment-provider-operation:");

                            assertThat(transaction.getProcessingExpiresAt())
                                    .isEqualTo(NOW.plus(LEASE_DURATION));
                        });

        Set<String> processingOwners =
                claimedTransactions.stream()
                        .map(PaymentTransaction::getProcessingOwner)
                        .collect(java.util.stream.Collectors.toSet());

        assertThat(processingOwners).hasSize(1);

        List<PaymentTransaction> persistedTransactions = transactionRepository.findAll();

        assertThat(persistedTransactions)
                .filteredOn(
                        transaction ->
                                transaction.getStatus() == PaymentTransactionStatus.PROCESSING)
                .hasSize(2);

        assertThat(persistedTransactions)
                .filteredOn(
                        transaction -> transaction.getStatus() == PaymentTransactionStatus.READY)
                .hasSize(1);
    }

    @Test
    void activeLeaseShouldNotBeClaimedAgain() {

        persistReadyCharge("charge:active-lease");

        List<PaymentTransaction> firstClaim = claimService.claimNextBatch();

        List<PaymentTransaction> secondClaim = claimService.claimNextBatch();

        assertThat(firstClaim).hasSize(1);
        assertThat(secondClaim).isEmpty();

        PaymentTransaction persistedTransaction =
                transactionRepository.findById(firstClaim.get(0).getId()).orElseThrow();

        assertThat(persistedTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.PROCESSING);

        assertThat(persistedTransaction.getProcessingOwner())
                .isEqualTo(firstClaim.get(0).getProcessingOwner());
    }

    @Test
    void concurrentClaimsShouldSkipLockedRows() throws Exception {

        for (int index = 1; index <= 4; index++) {
            persistReadyCharge("charge:skip-locked:" + index);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CountDownLatch firstWorkerLockedRows = new CountDownLatch(1);

        CountDownLatch releaseFirstWorker = new CountDownLatch(1);

        try {
            Future<List<UUID>> firstWorker =
                    executorService.submit(
                            () ->
                                    claimInsideTransaction(
                                            "mysql-worker-1",
                                            firstWorkerLockedRows,
                                            releaseFirstWorker));

            await(firstWorkerLockedRows);

            Future<List<UUID>> secondWorker =
                    executorService.submit(
                            () -> claimInsideTransaction("mysql-worker-2", null, null));

            List<UUID> secondWorkerIds;

            try {
                secondWorkerIds = secondWorker.get(10, TimeUnit.SECONDS);
            } finally {
                releaseFirstWorker.countDown();
            }

            List<UUID> firstWorkerIds = firstWorker.get(10, TimeUnit.SECONDS);

            assertThat(firstWorkerIds).hasSize(2);
            assertThat(secondWorkerIds).hasSize(2);

            assertThat(firstWorkerIds).doesNotContainAnyElementsOf(secondWorkerIds);

            List<UUID> allClaimedIds = new ArrayList<>(firstWorkerIds);

            allClaimedIds.addAll(secondWorkerIds);

            assertThat(allClaimedIds).doesNotHaveDuplicates().hasSize(4);

            List<PaymentTransaction> persistedTransactions = transactionRepository.findAll();

            assertThat(persistedTransactions)
                    .allSatisfy(
                            transaction ->
                                    assertThat(transaction.getStatus())
                                            .isEqualTo(PaymentTransactionStatus.PROCESSING));

            assertThat(persistedTransactions)
                    .extracting(PaymentTransaction::getProcessingOwner)
                    .containsExactlyInAnyOrder(
                            "mysql-worker-1", "mysql-worker-1", "mysql-worker-2", "mysql-worker-2");

        } finally {
            releaseFirstWorker.countDown();
            executorService.shutdownNow();
        }
    }

    @Test
    void expiredLeaseShouldBeReclaimedWithSameIdempotencyKey() {

        PaymentTransaction originalTransaction = persistReadyCharge("charge:lease-recovery");

        List<PaymentTransaction> firstClaim = claimService.claimNextBatch();

        assertThat(firstClaim).hasSize(1);

        PaymentTransaction firstClaimedTransaction = firstClaim.get(0);

        String firstOwner = firstClaimedTransaction.getProcessingOwner();

        expireLease(originalTransaction.getIdempotencyKey());

        List<PaymentTransaction> secondClaim = claimService.claimNextBatch();

        assertThat(secondClaim).hasSize(1);

        PaymentTransaction reclaimedTransaction = secondClaim.get(0);

        assertThat(reclaimedTransaction.getId()).isEqualTo(originalTransaction.getId());

        assertThat(reclaimedTransaction.getIdempotencyKey())
                .isEqualTo(originalTransaction.getIdempotencyKey());

        assertThat(reclaimedTransaction.getProcessingOwner()).isNotEqualTo(firstOwner);

        assertThat(reclaimedTransaction.getProcessingExpiresAt())
                .isEqualTo(NOW.plus(LEASE_DURATION));
    }

    @Test
    void crashAfterProviderAcceptanceShouldRetrySameOperationIdempotently() {

        PaymentTransaction transaction = persistReadyCharge("charge:provider-crash-window");

        PaymentTransaction firstClaim = claimService.claimNextBatch().get(0);

        ClaimedProviderChargeOperation firstOperation =
                preparationService.prepareCharge(
                        firstClaim.getId(), firstClaim.getProcessingOwner());

        ProviderChargeResult firstProviderResult = operationExecutor.execute(firstOperation);

        /*
         * Simulate process termination here:
         *
         * - provider already accepted the request;
         * - resultApplicationService.apply(...) was never called;
         * - transaction remains PROCESSING;
         * - processing lease later expires.
         */
        expireLease(transaction.getIdempotencyKey());

        PaymentTransaction secondClaim = claimService.claimNextBatch().get(0);

        ClaimedProviderChargeOperation secondOperation =
                preparationService.prepareCharge(
                        secondClaim.getId(), secondClaim.getProcessingOwner());

        ProviderChargeResult secondProviderResult = operationExecutor.execute(secondOperation);

        assertThat(secondOperation.transactionId()).isEqualTo(firstOperation.transactionId());

        assertThat(secondOperation.idempotencyKey()).isEqualTo(firstOperation.idempotencyKey());

        assertThat(secondOperation.processingOwner())
                .isNotEqualTo(firstOperation.processingOwner());

        assertThat(secondProviderResult).isEqualTo(firstProviderResult);

        assertThat(secondProviderResult.providerReference())
                .isEqualTo(firstProviderResult.providerReference());

        Payment persistedPayment =
                paymentRepository.findById(secondOperation.command().paymentId()).orElseThrow();

        assertThat(persistedPayment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);

        PaymentTransaction persistedTransaction =
                transactionRepository.findById(transaction.getId()).orElseThrow();

        assertThat(persistedTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.PROCESSING);

        assertThat(persistedTransaction.getProcessingOwner())
                .isEqualTo(secondOperation.processingOwner());
    }

    private List<UUID> claimInsideTransaction(
            String owner, CountDownLatch locksAcquired, CountDownLatch releaseTransaction) {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

        return transactionTemplate.execute(
                status -> {
                    List<PaymentTransaction> transactions =
                            transactionRepository.findReadyTransactions(2);

                    transactions.forEach(
                            transaction -> transaction.claim(owner, NOW, NOW.plus(LEASE_DURATION)));

                    transactionRepository.saveAllAndFlush(transactions);

                    if (locksAcquired != null) {
                        locksAcquired.countDown();
                    }

                    if (releaseTransaction != null) {
                        await(releaseTransaction);
                    }

                    return transactions.stream().map(PaymentTransaction::getId).toList();
                });
    }

    private void expireLease(String idempotencyKey) {

        int updatedRows =
                jdbcTemplate.update(
                        """
                        UPDATE payment_transactions
                        SET processing_expires_at = ?
                        WHERE provider = 'MOCK'
                          AND idempotency_key = ?
                          AND status = 'PROCESSING'
                        """,
                        Timestamp.valueOf(NOW.minusSeconds(1).toLocalDateTime()),
                        idempotencyKey);

        assertThat(updatedRows).isEqualTo(1);
    }

    private PaymentTransaction persistReadyCharge(String idempotencyKey) {

        Payment payment =
                new Payment(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        1,
                        new BigDecimal("125000.00"),
                        "VND",
                        "MOCK",
                        NOW.plusMinutes(10),
                        NOW.minusMinutes(1),
                        UuidGenerator.next(),
                        UuidGenerator.next());

        paymentRepository.saveAndFlush(payment);

        PaymentTransaction transaction =
                new PaymentTransaction(
                        payment.getId(),
                        payment.getProvider(),
                        PaymentTransactionType.CHARGE,
                        1,
                        payment.getAmount(),
                        payment.getCurrency(),
                        idempotencyKey,
                        NOW.minusMinutes(1));

        return transactionRepository.saveAndFlush(transaction);
    }

    private static void await(CountDownLatch latch) {

        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);

            if (!completed) {
                throw new AssertionError("Timed out waiting for concurrent claim");
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new AssertionError("Concurrent claim was interrupted", exception);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock providerOperationIntegrationClock() {

            return Clock.fixed(Instant.parse("2026-09-08T10:00:00Z"), ZoneOffset.UTC);
        }
    }
}
