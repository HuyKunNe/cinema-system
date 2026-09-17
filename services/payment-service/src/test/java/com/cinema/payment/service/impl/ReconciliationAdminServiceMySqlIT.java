package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.payment.entity.FinancialAuditRecord;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.entity.ReconciliationCase;
import com.cinema.payment.enums.FinancialAuditAction;
import com.cinema.payment.enums.FinancialAuditActorType;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.enums.ReconciliationReason;
import com.cinema.payment.enums.ReconciliationResolution;
import com.cinema.payment.enums.ReconciliationStatus;
import com.cinema.payment.enums.RefundStatus;
import com.cinema.payment.model.ReconciliationRejectRequest;
import com.cinema.payment.model.ReconciliationResolveRequest;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.repository.ReconciliationCaseRepository;
import com.cinema.payment.service.ReconciliationAdminService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Testcontainers
@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
class ReconciliationAdminServiceMySqlIT {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-17T04:00:00Z");

    private static final String PROCESSING_OWNER = "payment-refund-provider-operation:mysql-it";

    @Container @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.0");

    @Autowired private ReconciliationAdminService reconciliationAdminService;

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @Autowired private ReconciliationCaseRepository reconciliationCaseRepository;

    @MockitoSpyBean private FinancialAuditRecordRepository auditRepository;

    @AfterEach
    void cleanUp() {

        reset(auditRepository);

        /*
         * FK order:
         * audit -> reconciliation -> transaction -> payment
         */
        auditRepository.deleteAll();
        reconciliationCaseRepository.deleteAll();
        transactionRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    void concurrentResolveShouldAllowExactlyOneResolution() throws Exception {

        Fixture fixture = persistFixture();

        ReconciliationResolveRequest firstRequest =
                resolveSuccessRequest(
                        fixture.reconciliationCaseId(), "admin-1", "provider-refund-final-123");

        ReconciliationResolveRequest secondRequest =
                resolveSuccessRequest(
                        fixture.reconciliationCaseId(), "admin-2", "provider-refund-final-123");

        CountDownLatch startGate = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {

            Future<OperationOutcome> first =
                    executor.submit(
                            () -> {
                                startGate.await();

                                return executeResolve(firstRequest);
                            });

            Future<OperationOutcome> second =
                    executor.submit(
                            () -> {
                                startGate.await();

                                return executeResolve(secondRequest);
                            });

            startGate.countDown();

            OperationOutcome firstOutcome = first.get(10, TimeUnit.SECONDS);

            OperationOutcome secondOutcome = second.get(10, TimeUnit.SECONDS);

            long successCount =
                    List.of(firstOutcome, secondOutcome).stream()
                            .filter(OperationOutcome::success)
                            .count();

            long conflictCount =
                    List.of(firstOutcome, secondOutcome).stream()
                            .filter(outcome -> outcome.failure() instanceof ConflictException)
                            .count();

            assertThat(successCount).isEqualTo(1);
            assertThat(conflictCount).isEqualTo(1);
        }

        /*
         * Read fresh state from MySQL after both transactions complete.
         */
        Payment payment = paymentRepository.findById(fixture.paymentId()).orElseThrow();

        PaymentTransaction transaction =
                transactionRepository.findById(fixture.transactionId()).orElseThrow();

        ReconciliationCase reconciliationCase =
                reconciliationCaseRepository.findById(fixture.reconciliationCaseId()).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.SUCCEEDED);

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);

        assertThat(transaction.getProviderReference()).isEqualTo("provider-refund-final-123");

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.RESOLVED);

        assertThat(reconciliationCase.getResolution())
                .isEqualTo(ReconciliationResolution.REFUND_SUCCEEDED);

        List<FinancialAuditRecord> audits =
                auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(fixture.paymentId());

        assertThat(audits)
                .filteredOn(
                        audit -> audit.getAction() == FinancialAuditAction.RECONCILIATION_RESOLVED)
                .hasSize(1);
    }

    @Test
    void concurrentRejectShouldAllowExactlyOneRejection() throws Exception {

        Fixture fixture = persistFixture();

        ReconciliationRejectRequest firstRequest =
                rejectRequest(fixture.reconciliationCaseId(), "admin-1");

        ReconciliationRejectRequest secondRequest =
                rejectRequest(fixture.reconciliationCaseId(), "admin-2");

        CountDownLatch startGate = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {

            Future<OperationOutcome> first =
                    executor.submit(
                            () -> {
                                startGate.await();

                                return executeReject(firstRequest);
                            });

            Future<OperationOutcome> second =
                    executor.submit(
                            () -> {
                                startGate.await();

                                return executeReject(secondRequest);
                            });

            startGate.countDown();

            OperationOutcome firstOutcome = first.get(10, TimeUnit.SECONDS);

            OperationOutcome secondOutcome = second.get(10, TimeUnit.SECONDS);

            long successCount =
                    List.of(firstOutcome, secondOutcome).stream()
                            .filter(OperationOutcome::success)
                            .count();

            long conflictCount =
                    List.of(firstOutcome, secondOutcome).stream()
                            .filter(outcome -> outcome.failure() instanceof ConflictException)
                            .count();

            assertThat(successCount).isEqualTo(1);

            assertThat(conflictCount).isEqualTo(1);
        }

        /*
         * Rejecting reconciliation is not a financial result.
         * Read fresh committed state after both transactions complete.
         */
        Payment payment = paymentRepository.findById(fixture.paymentId()).orElseThrow();

        PaymentTransaction transaction =
                transactionRepository.findById(fixture.transactionId()).orElseThrow();

        ReconciliationCase reconciliationCase =
                reconciliationCaseRepository.findById(fixture.reconciliationCaseId()).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.PENDING_PROVIDER);

        assertThat(transaction.getProviderReference()).isEqualTo("provider-refund-pending-123");

        assertThat(transaction.getCompletedAt()).isNull();

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.REJECTED);

        assertThat(reconciliationCase.getResolution()).isNull();

        assertThat(reconciliationCase.getResolvedAt()).isNotNull();

        assertThat(reconciliationCase.getResolvedBy()).isIn("admin-1", "admin-2");

        List<FinancialAuditRecord> audits =
                auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(fixture.paymentId());

        assertThat(audits)
                .filteredOn(
                        audit -> audit.getAction() == FinancialAuditAction.RECONCILIATION_REJECTED)
                .hasSize(1);
    }

    @Test
    void auditFailureShouldRollbackEntireResolutionTransaction() {

        Fixture fixture = persistFixture();

        /*
         * Force the append-only audit write to fail after the service has
         * mutated Payment, PaymentTransaction and ReconciliationCase.
         */
        doThrow(new DataIntegrityViolationException("forced audit failure"))
                .when(auditRepository)
                .save(any(FinancialAuditRecord.class));

        ReconciliationResolveRequest request =
                resolveSuccessRequest(
                        fixture.reconciliationCaseId(),
                        "rollback-admin",
                        "provider-refund-final-rollback");

        assertThatThrownBy(() -> reconciliationAdminService.resolve(request))
                .isInstanceOf(DataIntegrityViolationException.class);

        /*
         * Remove spy behavior before reading/cleanup.
         */
        reset(auditRepository);

        Payment payment = paymentRepository.findById(fixture.paymentId()).orElseThrow();

        PaymentTransaction transaction =
                transactionRepository.findById(fixture.transactionId()).orElseThrow();

        ReconciliationCase reconciliationCase =
                reconciliationCaseRepository.findById(fixture.reconciliationCaseId()).orElseThrow();

        /*
         * Everything must be exactly as it was before resolve().
         */
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.PENDING_PROVIDER);

        assertThat(transaction.getProviderReference()).isEqualTo("provider-refund-pending-123");

        assertThat(transaction.getCompletedAt()).isNull();

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.OPEN);

        assertThat(reconciliationCase.getResolution()).isNull();
        assertThat(reconciliationCase.getResolvedAt()).isNull();
        assertThat(reconciliationCase.getResolvedBy()).isNull();

        List<FinancialAuditRecord> audits =
                auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(fixture.paymentId());

        assertThat(audits)
                .filteredOn(
                        audit -> audit.getAction() == FinancialAuditAction.RECONCILIATION_RESOLVED)
                .isEmpty();
    }

    @Test
    void auditFailureShouldRollbackEntireRejectionTransaction() {

        Fixture fixture = persistFixture();

        /*
         * Force audit persistence to fail after ReconciliationCase.reject()
         * has mutated the managed entity.
         */
        doThrow(new DataIntegrityViolationException("forced audit failure"))
                .when(auditRepository)
                .save(any(FinancialAuditRecord.class));

        ReconciliationRejectRequest request =
                rejectRequest(fixture.reconciliationCaseId(), "rollback-admin");

        assertThatThrownBy(() -> reconciliationAdminService.reject(request))
                .isInstanceOf(DataIntegrityViolationException.class);

        /*
         * Remove spy behavior before reading committed database state.
         */
        reset(auditRepository);

        Payment payment = paymentRepository.findById(fixture.paymentId()).orElseThrow();

        PaymentTransaction transaction =
                transactionRepository.findById(fixture.transactionId()).orElseThrow();

        ReconciliationCase reconciliationCase =
                reconciliationCaseRepository.findById(fixture.reconciliationCaseId()).orElseThrow();

        /*
         * Reject is not a financial result, and failed audit persistence
         * must roll back the case mutation as well.
         */
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.PENDING_PROVIDER);

        assertThat(transaction.getProviderReference()).isEqualTo("provider-refund-pending-123");

        assertThat(transaction.getCompletedAt()).isNull();

        assertThat(reconciliationCase.getStatus()).isEqualTo(ReconciliationStatus.OPEN);

        assertThat(reconciliationCase.getResolution()).isNull();

        assertThat(reconciliationCase.getResolvedAt()).isNull();

        assertThat(reconciliationCase.getResolvedByType()).isNull();

        assertThat(reconciliationCase.getResolvedBy()).isNull();

        List<FinancialAuditRecord> audits =
                auditRepository.findAllByPaymentIdOrderByOccurredAtAscIdAsc(fixture.paymentId());

        assertThat(audits)
                .filteredOn(
                        audit -> audit.getAction() == FinancialAuditAction.RECONCILIATION_REJECTED)
                .isEmpty();
    }

    private OperationOutcome executeResolve(ReconciliationResolveRequest request) {

        try {
            reconciliationAdminService.resolve(request);

            return OperationOutcome.succeeded();

        } catch (Throwable throwable) {

            return OperationOutcome.failed(throwable);
        }
    }

    private OperationOutcome executeReject(ReconciliationRejectRequest request) {

        try {
            reconciliationAdminService.reject(request);

            return OperationOutcome.succeeded();

        } catch (Throwable throwable) {

            return OperationOutcome.failed(throwable);
        }
    }

    private static ReconciliationRejectRequest rejectRequest(
            UUID reconciliationCaseId, String actorId) {

        return new ReconciliationRejectRequest(
                reconciliationCaseId,
                FinancialAuditActorType.USER,
                actorId,
                "Insufficient evidence to determine provider outcome",
                UuidGenerator.next(),
                NOW);
    }

    private Fixture persistFixture() {

        OffsetDateTime requestedAt = NOW.minusMinutes(10);

        Payment payment =
                new Payment(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        1,
                        new BigDecimal("125000.00"),
                        "VND",
                        "MOCK",
                        NOW.plusMinutes(10),
                        requestedAt,
                        UuidGenerator.next(),
                        UuidGenerator.next());

        payment.startProcessing();

        payment.completeProviderSuccess("provider-charge-123", NOW.minusMinutes(5));

        payment.requestRefund();

        payment = paymentRepository.saveAndFlush(payment);

        PaymentTransaction transaction =
                new PaymentTransaction(
                        payment.getId(),
                        payment.getProvider(),
                        PaymentTransactionType.REFUND,
                        1,
                        payment.getAmount(),
                        payment.getCurrency(),
                        "refund:" + payment.getId(),
                        NOW.minusMinutes(2));

        transaction.claim(PROCESSING_OWNER, NOW.minusMinutes(2), NOW.plusMinutes(1));

        transaction.markPendingProvider(
                PROCESSING_OWNER, "provider-refund-pending-123", null, null);

        transaction = transactionRepository.saveAndFlush(transaction);

        ReconciliationCase reconciliationCase =
                new ReconciliationCase(
                        payment.getId(),
                        transaction.getId(),
                        ReconciliationReason.REFUND_PROVIDER_PENDING,
                        payment.getProvider(),
                        transaction.getProviderReference(),
                        NOW.minusMinutes(1));

        reconciliationCase = reconciliationCaseRepository.saveAndFlush(reconciliationCase);

        return new Fixture(payment.getId(), transaction.getId(), reconciliationCase.getId());
    }

    private static ReconciliationResolveRequest resolveSuccessRequest(
            UUID reconciliationCaseId, String actorId, String providerReference) {

        return new ReconciliationResolveRequest(
                reconciliationCaseId,
                ReconciliationResolution.REFUND_SUCCEEDED,
                FinancialAuditActorType.USER,
                actorId,
                "Confirmed from provider dashboard",
                providerReference,
                null,
                null,
                UuidGenerator.next(),
                NOW);
    }

    private record Fixture(UUID paymentId, UUID transactionId, UUID reconciliationCaseId) {}

    private record OperationOutcome(boolean success, Throwable failure) {

        private static OperationOutcome succeeded() {
            return new OperationOutcome(true, null);
        }

        private static OperationOutcome failed(Throwable failure) {

            return new OperationOutcome(false, failure);
        }
    }
}
