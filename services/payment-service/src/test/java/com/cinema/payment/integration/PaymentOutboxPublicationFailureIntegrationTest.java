package com.cinema.payment.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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

@Import({
    OutboxConfiguration.class,
    PaymentOutboxPublicationFailureIntegrationTest.MutableClockConfiguration.class
})
@TestPropertySource(
        properties = {
            "spring.cloud.config.enabled=false",
            "eureka.client.enabled=false",

            "cinema.payment.provider=MOCK",
            "cinema.payment.kafka.enabled=false",
            "cinema.payment.provider-operation.scheduling-enabled=false",
            "cinema.outbox.enabled=false",
            "cinema.outbox.producer=payment-service",
            "cinema.outbox.batch-size=10",
            "cinema.outbox.lease-duration=30s",
            "cinema.outbox.maximum-attempts=5",
            "cinema.outbox.base-retry-delay=1s",
            "cinema.outbox.maximum-retry-delay=10s",
            "cinema.outbox.maximum-jitter=0ms"
        })
class PaymentOutboxPublicationFailureIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final Instant INITIAL_INSTANT =
            Instant.parse("2026-09-18T10:00:00Z");

    private static final OffsetDateTime NOW =
            OffsetDateTime.ofInstant(
                    INITIAL_INSTANT,
                    ZoneOffset.UTC);

    private static final BigDecimal AMOUNT =
            new BigDecimal("180000.00");

    private static final String PROVIDER_REFERENCE =
            "mock-provider-reference-publication-failure";

    private static final String KAFKA_FAILURE_MESSAGE =
            "Kafka broker unavailable";

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private PaymentSucceededOutboxFactory succeededOutboxFactory;

    @Autowired
    private OutboxClaimService claimService;

    @Autowired
    private OutboxPublisher publisher;

    @Autowired
    private OutboxAcknowledgementService acknowledgementService;

    @Autowired
    private MutableClock clock;

    @MockitoBean
    private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setUp() {

        clock.setInstant(
                INITIAL_INSTANT);

        outboxRepository.deleteAllInBatch();
    }

    @Test
    void kafkaPublicationFailureShouldPersistFailedStateAndBecomeRetryable()
            throws Exception {

        Payment payment =
                persistSucceededPayment();

        OutboxEventEntity event =
                succeededOutboxFactory.create(
                        payment);

        outboxRepository.saveAndFlush(
                event);

        assertThat(event.getStatus())
                .isEqualTo(
                        OutboxStatus.PENDING);

        assertThat(event.getRetryCount())
                .isZero();

        RuntimeException kafkaFailure =
                new RuntimeException(
                        KAFKA_FAILURE_MESSAGE);

        when(
                        kafkaProducerService.send(
                                eq(PaymentEventContract.PAYMENT_SUCCEEDED),
                                eq(payment.getBookingId().toString()),
                                any(OutboxEventMessage.class)))
                .thenReturn(
                        CompletableFuture.failedFuture(
                                kafkaFailure));

        OutboxScheduler scheduler =
                new OutboxScheduler(
                        claimService,
                        publisher,
                        acknowledgementService);

        scheduler.publishPendingEvents();

        verify(kafkaProducerService)
                .send(
                        eq(PaymentEventContract.PAYMENT_SUCCEEDED),
                        eq(payment.getBookingId().toString()),
                        any(OutboxEventMessage.class));

        OutboxEventEntity failedEvent =
                outboxRepository
                        .findById(event.getId())
                        .orElseThrow();

        assertThat(failedEvent.getStatus())
                .isEqualTo(
                        OutboxStatus.FAILED);

        assertThat(failedEvent.getRetryCount())
                .isEqualTo(1);

        assertThat(failedEvent.getNextAttemptAt())
                .isEqualTo(
                        NOW.plusSeconds(1));

        assertThat(failedEvent.getLastError())
                .isEqualTo(
                        KAFKA_FAILURE_MESSAGE);

        assertThat(failedEvent.getProcessingOwner())
                .isNull();

        assertThat(failedEvent.getProcessingStartedAt())
                .isNull();

        assertThat(failedEvent.getProcessingExpiresAt())
                .isNull();

        assertThat(failedEvent.getPublishedAt())
                .isNull();

        List<OutboxEventEntity> prematureRetry =
                claimService.claimNextBatch();

        assertThat(prematureRetry)
                .isEmpty();

        clock.advance(
                Duration.ofSeconds(1));

        List<OutboxEventEntity> retryClaim =
                claimService.claimNextBatch();

        assertThat(retryClaim)
                .hasSize(1);

        OutboxEventEntity retriedEvent =
                retryClaim.getFirst();

        assertThat(retriedEvent.getId())
                .isEqualTo(
                        event.getId());

        assertThat(retriedEvent.getStatus())
                .isEqualTo(
                        OutboxStatus.PROCESSING);

        assertThat(retriedEvent.getRetryCount())
                .isEqualTo(1);

        assertThat(retriedEvent.getProcessingOwner())
                .isNotBlank();

        assertThat(retriedEvent.getProcessingStartedAt())
                .isEqualTo(
                        NOW.plusSeconds(1));

        assertThat(retriedEvent.getProcessingExpiresAt())
                .isEqualTo(
                        NOW.plusSeconds(31));

        assertThat(retriedEvent.getLastError())
                .isNull();
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
                        NOW.plusMinutes(30),
                        NOW.minusMinutes(1),
                        UuidGenerator.next(),
                        UuidGenerator.next());

        payment.startProcessing();

        payment.completeProviderSuccess(
                PROVIDER_REFERENCE,
                NOW);

        assertThat(payment.getStatus())
                .isEqualTo(
                        PaymentStatus.SUCCEEDED);

        return paymentRepository.saveAndFlush(
                payment);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableClockConfiguration {

        @Bean
        @Primary
        MutableClock paymentOutboxFailureClock() {

            return new MutableClock(
                    INITIAL_INSTANT,
                    ZoneOffset.UTC);
        }
    }

    static final class MutableClock extends Clock {

        private volatile Instant instant;

        private final ZoneId zone;

        MutableClock(
                Instant instant,
                ZoneId zone) {

            this.instant = instant;
            this.zone = zone;
        }

        void setInstant(
                Instant instant) {

            this.instant = instant;
        }

        void advance(
                Duration duration) {

            instant =
                    instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {

            return zone;
        }

        @Override
        public Clock withZone(
                ZoneId zone) {

            return new MutableClock(
                    instant,
                    zone);
        }

        @Override
        public Instant instant() {

            return instant;
        }
    }
}
