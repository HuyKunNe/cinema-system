package com.cinema.payment.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.kafka.producer.KafkaProducerService;
import com.cinema.common.outbox.acknowledgement.OutboxAcknowledgementService;
import com.cinema.common.outbox.claim.OutboxClaimService;
import com.cinema.common.outbox.config.OutboxConfiguration;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.publisher.OutboxPublisher;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.outbox.scheduler.OutboxScheduler;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.event.PaymentEventContract;
import com.cinema.payment.event.PaymentSucceededOutboxFactory;
import com.cinema.payment.repository.PaymentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Import({
    OutboxConfiguration.class,
    PaymentOutboxRetryExhaustionIntegrationTest.MutableClockConfiguration.class
})
@TestPropertySource(
        properties = {
            "spring.cloud.config.enabled=false",
            "eureka.client.enabled=false",
            "cinema.payment.provider=MOCK",
            "cinema.payment.kafka.enabled=false",
            "cinema.payment.provider-operation.scheduling-enabled=false",

            /*
             * Scheduler execution is controlled explicitly by the test.
             */
            "cinema.outbox.enabled=false",
            "cinema.outbox.producer=payment-service",
            "cinema.outbox.batch-size=10",
            "cinema.outbox.lease-duration=30s",

            /*
             * Three TOTAL failed publication attempts are allowed.
             */
            "cinema.outbox.maximum-attempts=3",

            /*
             * Deterministic exponential retry:
             *
             * failure #1 -> +1s
             * failure #2 -> +2s
             * failure #3 -> +4s
             */
            "cinema.outbox.base-retry-delay=1s",
            "cinema.outbox.maximum-retry-delay=10s",
            "cinema.outbox.maximum-jitter=0ms"
        })
class PaymentOutboxRetryExhaustionIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final Instant INITIAL_INSTANT = Instant.parse("2026-09-18T10:00:00Z");

    private static final OffsetDateTime INITIAL_TIME =
            OffsetDateTime.ofInstant(INITIAL_INSTANT, ZoneOffset.UTC);

    private static final BigDecimal AMOUNT = new BigDecimal("180000.00");

    private static final String FAILURE_MESSAGE = "Kafka broker unavailable";

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private PaymentSucceededOutboxFactory succeededOutboxFactory;

    @Autowired private OutboxClaimService claimService;

    @Autowired private OutboxPublisher publisher;

    @Autowired private OutboxAcknowledgementService acknowledgementService;

    @Autowired private MutableClock clock;

    @MockitoBean private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setUp() {

        clock.setInstant(INITIAL_INSTANT);

        outboxRepository.deleteAllInBatch();

        paymentRepository.deleteAllInBatch();
    }

    @Test
    void exhaustedPublicationShouldNotBeClaimedAfterMaximumAttempts() throws Exception {

        Payment payment = persistSucceededPayment();

        OutboxEventEntity event = succeededOutboxFactory.create(payment);

        outboxRepository.saveAndFlush(event);

        when(kafkaProducerService.send(
                        eq(PaymentEventContract.PAYMENT_SUCCEEDED),
                        eq(payment.getBookingId().toString()),
                        any(OutboxEventMessage.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException(FAILURE_MESSAGE)));

        OutboxScheduler scheduler =
                new OutboxScheduler(claimService, publisher, acknowledgementService);

        /*
         * ============================================================
         * Attempt #1
         * ============================================================
         */

        scheduler.publishPendingEvents();

        OutboxEventEntity afterFirstFailure = loadEvent(event);

        assertThat(afterFirstFailure.getStatus()).isEqualTo(OutboxStatus.FAILED);

        assertThat(afterFirstFailure.getRetryCount()).isEqualTo(1);

        assertThat(afterFirstFailure.getNextAttemptAt()).isEqualTo(INITIAL_TIME.plusSeconds(1));

        assertThat(afterFirstFailure.getLastError()).isEqualTo(FAILURE_MESSAGE);

        assertLeaseCleared(afterFirstFailure);

        /*
         * Before t+1s it must not be claimable.
         */
        assertThat(claimService.claimNextBatch()).isEmpty();

        /*
         * ============================================================
         * Attempt #2
         * ============================================================
         */

        clock.advance(Duration.ofSeconds(1));

        scheduler.publishPendingEvents();

        OutboxEventEntity afterSecondFailure = loadEvent(event);

        assertThat(afterSecondFailure.getStatus()).isEqualTo(OutboxStatus.FAILED);

        assertThat(afterSecondFailure.getRetryCount()).isEqualTo(2);

        /*
         * The second publication attempt begins at t+1s.
         *
         * RetryPolicy receives currentRetryCount=1:
         *
         * 1 second * 2^1 = 2 seconds
         *
         * next attempt = t+3s.
         */
        assertThat(afterSecondFailure.getNextAttemptAt()).isEqualTo(INITIAL_TIME.plusSeconds(3));

        assertThat(afterSecondFailure.getLastError()).isEqualTo(FAILURE_MESSAGE);

        assertLeaseCleared(afterSecondFailure);

        /*
         * At t+1s the next retry must still be blocked.
         */
        assertThat(claimService.claimNextBatch()).isEmpty();

        /*
         * ============================================================
         * Attempt #3
         * ============================================================
         */

        clock.advance(Duration.ofSeconds(2));

        scheduler.publishPendingEvents();

        OutboxEventEntity exhaustedEvent = loadEvent(event);

        assertThat(exhaustedEvent.getStatus()).isEqualTo(OutboxStatus.FAILED);

        assertThat(exhaustedEvent.getRetryCount()).isEqualTo(3);

        /*
         * The third failure occurs at t+3s.
         *
         * RetryPolicy still calculates nextAttemptAt:
         *
         * 1 second * 2^2 = 4 seconds
         *
         * => t+7s.
         *
         * Exhaustion is enforced by retry_count < maximumAttempts,
         * not by clearing nextAttemptAt.
         */
        assertThat(exhaustedEvent.getNextAttemptAt()).isEqualTo(INITIAL_TIME.plusSeconds(7));

        assertThat(exhaustedEvent.getLastError()).isEqualTo(FAILURE_MESSAGE);

        assertThat(exhaustedEvent.getPublishedAt()).isNull();

        assertLeaseCleared(exhaustedEvent);

        /*
         * Exactly three publication attempts occurred.
         */
        verify(kafkaProducerService, times(3))
                .send(
                        eq(PaymentEventContract.PAYMENT_SUCCEEDED),
                        eq(payment.getBookingId().toString()),
                        any(OutboxEventMessage.class));

        /*
         * ============================================================
         * Exhaustion verification
         * ============================================================
         *
         * Move WELL BEYOND nextAttemptAt.
         *
         * Even though nextAttemptAt has elapsed, retry_count == 3 and
         * maximumAttempts == 3.
         *
         * Repository predicate:
         *
         * retry_count < maximumAttempts
         *
         * becomes:
         *
         * 3 < 3
         *
         * false.
         */
        clock.advance(Duration.ofHours(1));

        List<OutboxEventEntity> exhaustedClaim = claimService.claimNextBatch();

        assertThat(exhaustedClaim).isEmpty();

        /*
         * Running the worker again must not publish the exhausted event.
         */
        scheduler.publishPendingEvents();

        verify(kafkaProducerService, times(3))
                .send(
                        eq(PaymentEventContract.PAYMENT_SUCCEEDED),
                        eq(payment.getBookingId().toString()),
                        any(OutboxEventMessage.class));

        OutboxEventEntity finalState = loadEvent(event);

        assertThat(finalState.getStatus()).isEqualTo(OutboxStatus.FAILED);

        assertThat(finalState.getRetryCount()).isEqualTo(3);

        assertThat(finalState.getPublishedAt()).isNull();

        assertLeaseCleared(finalState);
    }

    private Payment persistSucceededPayment() {

        Payment payment =
                new Payment(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        1,
                        AMOUNT,
                        "VND",
                        "MOCK",
                        INITIAL_TIME.plusMinutes(30),
                        INITIAL_TIME.minusMinutes(1),
                        UuidGenerator.next(),
                        UuidGenerator.next());

        payment.startProcessing();

        payment.completeProviderSuccess("mock-provider-reference-retry-exhaustion", INITIAL_TIME);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        return paymentRepository.saveAndFlush(payment);
    }

    private OutboxEventEntity loadEvent(OutboxEventEntity event) {

        return outboxRepository.findById(event.getId()).orElseThrow();
    }

    private static void assertLeaseCleared(OutboxEventEntity event) {

        assertThat(event.getProcessingOwner()).isNull();

        assertThat(event.getProcessingStartedAt()).isNull();

        assertThat(event.getProcessingExpiresAt()).isNull();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableClockConfiguration {

        @Bean
        @Primary
        MutableClock paymentOutboxRetryExhaustionClock() {

            return new MutableClock(INITIAL_INSTANT, ZoneOffset.UTC);
        }
    }

    static final class MutableClock extends Clock {

        private volatile Instant instant;

        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {

            this.instant = instant;
            this.zone = zone;
        }

        void setInstant(Instant instant) {

            this.instant = instant;
        }

        void advance(Duration duration) {

            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {

            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {

            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {

            return instant;
        }
    }
}
