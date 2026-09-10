package com.cinema.payment.provider.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.provider.model.ProviderOutcome;
import com.cinema.payment.provider.webhook.model.PaymentProviderWebhookApplicationResult;
import com.cinema.payment.provider.webhook.model.ProviderWebhookApplicationDisposition;
import com.cinema.payment.provider.webhook.model.VerifiedProviderWebhook;
import com.cinema.payment.repository.PaymentProviderWebhookEventRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.service.PaymentProviderWebhookApplicationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Import(PaymentProviderWebhookMySqlIntegrationTest.FixedClockConfiguration.class)
@TestPropertySource(
        properties = {"cinema.payment.provider=MOCK", "cinema.payment.kafka.enabled=false"})
class PaymentProviderWebhookMySqlIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-10T09:00:00Z");

    private static final BigDecimal AMOUNT = new BigDecimal("125000.00");

    private static final String PROVIDER = "MOMO";

    private static final String PROVIDER_REFERENCE = "provider-reference-webhook-test";

    private static final String PROCESSING_OWNER = "payment-webhook-integration-worker";

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @Autowired private PaymentProviderWebhookEventRepository webhookEventRepository;

    @Autowired private PaymentProviderWebhookApplicationService applicationService;

    @BeforeEach
    void cleanDatabase() {
        webhookEventRepository.deleteAllInBatch();
        transactionRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
    }

    @Test
    void duplicateWebhookShouldCreateOneMarkerAndOneTransition() {
        TestAggregate aggregate = persistPendingAggregate();

        VerifiedProviderWebhook webhook = succeededWebhook("provider-event-duplicate");

        PaymentProviderWebhookApplicationResult firstResult = applicationService.apply(webhook);

        PaymentProviderWebhookApplicationResult secondResult = applicationService.apply(webhook);

        assertThat(firstResult.disposition())
                .isEqualTo(ProviderWebhookApplicationDisposition.APPLIED);

        assertThat(secondResult.disposition())
                .isEqualTo(ProviderWebhookApplicationDisposition.DUPLICATE);

        assertThat(
                        webhookEventRepository.countByProviderAndProviderEventId(
                                PROVIDER, webhook.providerEventId()))
                .isEqualTo(1);

        Payment persistedPayment =
                paymentRepository.findById(aggregate.payment().getId()).orElseThrow();

        PaymentTransaction persistedTransaction =
                transactionRepository.findById(aggregate.transaction().getId()).orElseThrow();

        assertThat(persistedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(persistedPayment.getProviderReference()).isEqualTo(PROVIDER_REFERENCE);

        assertThat(persistedTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);

        assertThat(persistedTransaction.getProviderEventId()).isEqualTo(webhook.providerEventId());
    }

    @Test
    void concurrentDuplicateWebhookShouldApplyExactlyOnce() throws Exception {

        TestAggregate aggregate = persistPendingAggregate();

        VerifiedProviderWebhook webhook = succeededWebhook("provider-event-concurrent");

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CountDownLatch workersReady = new CountDownLatch(2);
        CountDownLatch startWorkers = new CountDownLatch(1);

        try {
            Future<ProviderWebhookApplicationDisposition> firstWorker =
                    executorService.submit(
                            () -> applyAfterStart(webhook, workersReady, startWorkers));

            Future<ProviderWebhookApplicationDisposition> secondWorker =
                    executorService.submit(
                            () -> applyAfterStart(webhook, workersReady, startWorkers));

            await(workersReady);
            startWorkers.countDown();

            ProviderWebhookApplicationDisposition firstDisposition =
                    firstWorker.get(15, TimeUnit.SECONDS);

            ProviderWebhookApplicationDisposition secondDisposition =
                    secondWorker.get(15, TimeUnit.SECONDS);

            assertThat(List.of(firstDisposition, secondDisposition))
                    .containsExactlyInAnyOrder(
                            ProviderWebhookApplicationDisposition.APPLIED,
                            ProviderWebhookApplicationDisposition.DUPLICATE);

            assertThat(
                            webhookEventRepository.countByProviderAndProviderEventId(
                                    PROVIDER, webhook.providerEventId()))
                    .isEqualTo(1);

            Payment persistedPayment =
                    paymentRepository.findById(aggregate.payment().getId()).orElseThrow();

            PaymentTransaction persistedTransaction =
                    transactionRepository.findById(aggregate.transaction().getId()).orElseThrow();

            assertThat(persistedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

            assertThat(persistedTransaction.getStatus())
                    .isEqualTo(PaymentTransactionStatus.SUCCEEDED);

            assertThat(persistedTransaction.getProviderEventId())
                    .isEqualTo(webhook.providerEventId());

        } finally {
            startWorkers.countDown();
            executorService.shutdownNow();
        }
    }

    @Test
    void pendingThenSuccessShouldPreserveBothProviderEventMarkers() {
        TestAggregate aggregate = persistPendingAggregate();

        VerifiedProviderWebhook pendingWebhook = pendingWebhook("provider-event-pending");

        VerifiedProviderWebhook succeededWebhook = succeededWebhook("provider-event-succeeded");

        PaymentProviderWebhookApplicationResult pendingResult =
                applicationService.apply(pendingWebhook);

        PaymentProviderWebhookApplicationResult succeededResult =
                applicationService.apply(succeededWebhook);

        assertThat(pendingResult.disposition())
                .isEqualTo(ProviderWebhookApplicationDisposition.APPLIED);

        assertThat(succeededResult.disposition())
                .isEqualTo(ProviderWebhookApplicationDisposition.APPLIED);

        assertThat(webhookEventRepository.count()).isEqualTo(2);

        assertThat(
                        webhookEventRepository.countByProviderAndProviderEventId(
                                PROVIDER, pendingWebhook.providerEventId()))
                .isEqualTo(1);

        assertThat(
                        webhookEventRepository.countByProviderAndProviderEventId(
                                PROVIDER, succeededWebhook.providerEventId()))
                .isEqualTo(1);

        Payment persistedPayment =
                paymentRepository.findById(aggregate.payment().getId()).orElseThrow();

        PaymentTransaction persistedTransaction =
                transactionRepository.findById(aggregate.transaction().getId()).orElseThrow();

        assertThat(persistedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(persistedTransaction.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);

        assertThat(persistedTransaction.getProviderEventId())
                .isEqualTo(succeededWebhook.providerEventId());
    }

    @Test
    void failedApplicationShouldRollbackProviderEventMarker() {
        TestAggregate aggregate = persistInconsistentAggregate();

        VerifiedProviderWebhook webhook = succeededWebhook("provider-event-rollback");

        assertThatThrownBy(() -> applicationService.apply(webhook))
                .isInstanceOf(ConflictException.class);

        assertThat(
                        webhookEventRepository.countByProviderAndProviderEventId(
                                PROVIDER, webhook.providerEventId()))
                .isZero();

        Payment persistedPayment =
                paymentRepository.findById(aggregate.payment().getId()).orElseThrow();

        PaymentTransaction persistedTransaction =
                transactionRepository.findById(aggregate.transaction().getId()).orElseThrow();

        assertThat(persistedPayment.getStatus()).isEqualTo(PaymentStatus.RECEIVED);

        assertThat(persistedPayment.getProviderReference()).isNull();

        assertThat(persistedTransaction.getStatus())
                .isEqualTo(PaymentTransactionStatus.PENDING_PROVIDER);

        assertThat(persistedTransaction.getProviderEventId()).isNull();
    }

    private ProviderWebhookApplicationDisposition applyAfterStart(
            VerifiedProviderWebhook webhook,
            CountDownLatch workersReady,
            CountDownLatch startWorkers) {

        workersReady.countDown();
        await(startWorkers);

        return applicationService.apply(webhook).disposition();
    }

    private TestAggregate persistPendingAggregate() {
        Payment payment = newPayment();

        payment.startProcessing();

        payment.recordPendingProvider(PROVIDER_REFERENCE, NOW.minusSeconds(10));

        paymentRepository.saveAndFlush(payment);

        PaymentTransaction transaction = newTransaction(payment);

        transaction.claim(PROCESSING_OWNER, NOW.minusSeconds(20), NOW.plusMinutes(1));

        transaction.markPendingProvider(PROCESSING_OWNER, PROVIDER_REFERENCE, null, null);

        transactionRepository.saveAndFlush(transaction);

        return new TestAggregate(payment, transaction);
    }

    private TestAggregate persistInconsistentAggregate() {
        Payment payment = newPayment();

        paymentRepository.saveAndFlush(payment);

        PaymentTransaction transaction = newTransaction(payment);

        transaction.claim(PROCESSING_OWNER, NOW.minusSeconds(20), NOW.plusMinutes(1));

        transaction.markPendingProvider(PROCESSING_OWNER, PROVIDER_REFERENCE, null, null);

        transactionRepository.saveAndFlush(transaction);

        return new TestAggregate(payment, transaction);
    }

    private static Payment newPayment() {
        return new Payment(
                UuidGenerator.next(),
                UuidGenerator.next(),
                1,
                AMOUNT,
                "VND",
                PROVIDER,
                NOW.plusMinutes(10),
                NOW.minusMinutes(1),
                UuidGenerator.next(),
                UuidGenerator.next());
    }

    private static PaymentTransaction newTransaction(Payment payment) {

        return new PaymentTransaction(
                payment.getId(),
                PROVIDER,
                PaymentTransactionType.CHARGE,
                1,
                AMOUNT,
                "VND",
                "charge:" + payment.getId(),
                NOW.minusMinutes(1));
    }

    private static VerifiedProviderWebhook succeededWebhook(String providerEventId) {

        return new VerifiedProviderWebhook(
                PROVIDER,
                providerEventId,
                PROVIDER_REFERENCE,
                ProviderOutcome.SUCCEEDED,
                AMOUNT,
                "VND",
                NOW.minusSeconds(1),
                null,
                null);
    }

    private static VerifiedProviderWebhook pendingWebhook(String providerEventId) {

        return new VerifiedProviderWebhook(
                PROVIDER,
                providerEventId,
                PROVIDER_REFERENCE,
                ProviderOutcome.PENDING,
                AMOUNT,
                "VND",
                NOW.minusSeconds(1),
                null,
                null);
    }

    private static void await(CountDownLatch latch) {
        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);

            if (!completed) {
                throw new AssertionError("Timed out waiting for concurrent webhook processing");
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new AssertionError("Concurrent webhook processing was interrupted", exception);
        }
    }

    private record TestAggregate(Payment payment, PaymentTransaction transaction) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock paymentWebhookIntegrationClock() {
            return Clock.fixed(Instant.parse("2026-09-10T09:00:00Z"), ZoneOffset.UTC);
        }
    }
}
