package com.cinema.booking.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.outbox.acknowledgement.DefaultOutboxAcknowledgementService;
import com.cinema.common.outbox.acknowledgement.OutboxAcknowledgementService;
import com.cinema.common.outbox.claim.DefaultOutboxClaimService;
import com.cinema.common.outbox.claim.OutboxClaimService;
import com.cinema.common.outbox.config.OutboxProperties;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.outbox.retry.OutboxRetryPolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"})
@Testcontainers(disabledWithoutDocker = true)
@EntityScan(basePackageClasses = OutboxEventEntity.class)
@EnableJpaRepositories(basePackageClasses = OutboxRepository.class)
@Import({
    DefaultOutboxClaimService.class,
    DefaultOutboxAcknowledgementService.class,
    OutboxRetryPolicy.class,
    OutboxClaimIntegrationTest.TestConfiguration.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxClaimIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-21T10:00:00Z");

    private static final OffsetDateTime NOW_OFFSET = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

    @Container
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("cinema_booking_test")
                    .withUsername("cinema")
                    .withPassword("cinema");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);

        registry.add("spring.datasource.username", MYSQL::getUsername);

        registry.add("spring.datasource.password", MYSQL::getPassword);

        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private OutboxRepository repository;

    @Autowired private OutboxClaimService claimService;

    @Autowired private OutboxAcknowledgementService acknowledgementService;

    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanDatabase() {

        repository.deleteAll();
        repository.flush();
    }

    @Test
    void sequentialClaimersShouldNotClaimSameEvent() {

        OutboxEventEntity event = repository.saveAndFlush(newEvent());

        List<OutboxEventEntity> firstClaim = claimService.claimNextBatch();

        List<OutboxEventEntity> secondClaim = claimService.claimNextBatch();

        assertThat(firstClaim).extracting(OutboxEventEntity::getId).containsExactly(event.getId());

        assertThat(secondClaim).isEmpty();

        OutboxEventEntity persisted = repository.findById(event.getId()).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(OutboxStatus.PROCESSING);

        assertThat(persisted.getProcessingOwner())
                .isEqualTo(firstClaim.getFirst().getProcessingOwner());
    }

    @Test
    void lockedEventShouldBeSkippedByConcurrentClaimer() throws Exception {

        OutboxEventEntity event = repository.saveAndFlush(newEvent());

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch rowLocked = new CountDownLatch(1);

        CountDownLatch releaseLock = new CountDownLatch(1);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try {
            Future<List<OutboxEventEntity>> lockingTransaction =
                    executor.submit(
                            () ->
                                    transactionTemplate.execute(
                                            status -> {
                                                List<OutboxEventEntity> locked =
                                                        repository.findClaimableEvents(
                                                                NOW_OFFSET, 5, 100);

                                                rowLocked.countDown();

                                                await(releaseLock);

                                                return locked;
                                            }));

            assertThat(rowLocked.await(10, TimeUnit.SECONDS)).isTrue();

            Future<List<OutboxEventEntity>> competingClaim =
                    executor.submit(claimService::claimNextBatch);

            List<OutboxEventEntity> competingResult = competingClaim.get(10, TimeUnit.SECONDS);

            assertThat(competingResult).isEmpty();

            releaseLock.countDown();

            List<OutboxEventEntity> locked = lockingTransaction.get(10, TimeUnit.SECONDS);

            assertThat(locked).extracting(OutboxEventEntity::getId).containsExactly(event.getId());

        } finally {
            releaseLock.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void activeProcessingLeaseShouldNotBeStolen() {

        OutboxEventEntity event = newEvent();

        event.claim("active-owner", NOW_OFFSET.minusSeconds(5), NOW_OFFSET.plusSeconds(25));

        repository.saveAndFlush(event);

        List<OutboxEventEntity> claimed = claimService.claimNextBatch();

        assertThat(claimed).isEmpty();

        OutboxEventEntity persisted = repository.findById(event.getId()).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(OutboxStatus.PROCESSING);

        assertThat(persisted.getProcessingOwner()).isEqualTo("active-owner");
    }

    @Test
    void expiredProcessingLeaseShouldBeRecovered() {

        OutboxEventEntity event = newEvent();

        event.claim("abandoned-owner", NOW_OFFSET.minusMinutes(2), NOW_OFFSET.minusMinutes(1));

        repository.saveAndFlush(event);

        List<OutboxEventEntity> claimed = claimService.claimNextBatch();

        assertThat(claimed).hasSize(1);

        OutboxEventEntity recovered = claimed.getFirst();

        assertThat(recovered.getId()).isEqualTo(event.getId());

        assertThat(recovered.getStatus()).isEqualTo(OutboxStatus.PROCESSING);

        assertThat(recovered.getProcessingOwner()).startsWith("outbox-claim:");

        assertThat(recovered.getProcessingOwner()).isNotEqualTo("abandoned-owner");

        assertThat(recovered.getProcessingStartedAt()).isEqualTo(NOW_OFFSET);

        assertThat(recovered.getProcessingExpiresAt()).isEqualTo(NOW_OFFSET.plusSeconds(30));
    }

    @Test
    void staleSuccessfulAcknowledgementShouldNotOverwriteNewClaim() {

        OutboxEventEntity event = newEvent();

        String staleOwner = "abandoned-owner";

        event.claim(staleOwner, NOW_OFFSET.minusMinutes(2), NOW_OFFSET.minusMinutes(1));

        repository.saveAndFlush(event);

        OutboxEventEntity recovered = claimService.claimNextBatch().getFirst();

        String currentOwner = recovered.getProcessingOwner();

        boolean acknowledged = acknowledgementService.acknowledgeSuccess(event.getId(), staleOwner);

        assertThat(acknowledged).isFalse();

        OutboxEventEntity persisted = repository.findById(event.getId()).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(OutboxStatus.PROCESSING);

        assertThat(persisted.getProcessingOwner()).isEqualTo(currentOwner);

        assertThat(persisted.getPublishedAt()).isNull();
    }

    @Test
    void matchingAcknowledgementShouldMarkEventSent() {

        OutboxEventEntity claimed = saveAndClaim();

        boolean acknowledged =
                acknowledgementService.acknowledgeSuccess(
                        claimed.getId(), claimed.getProcessingOwner());

        assertThat(acknowledged).isTrue();

        OutboxEventEntity persisted = repository.findById(claimed.getId()).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(OutboxStatus.SENT);

        assertThat(persisted.getPublishedAt()).isEqualTo(NOW_OFFSET);

        assertThat(persisted.getNextAttemptAt()).isNull();
        assertThat(persisted.getProcessingOwner()).isNull();
        assertThat(persisted.getProcessingStartedAt()).isNull();
        assertThat(persisted.getProcessingExpiresAt()).isNull();
    }

    @Test
    void failedAcknowledgementShouldDelayNextClaim() {

        OutboxEventEntity claimed = saveAndClaim();

        boolean acknowledged =
                acknowledgementService.acknowledgeFailure(
                        claimed.getId(),
                        claimed.getProcessingOwner(),
                        claimed.getRetryCount(),
                        new RuntimeException("Kafka broker unavailable"));

        assertThat(acknowledged).isTrue();

        OutboxEventEntity failed = repository.findById(claimed.getId()).orElseThrow();

        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.FAILED);

        assertThat(failed.getRetryCount()).isEqualTo(1);

        assertThat(failed.getNextAttemptAt()).isAfter(NOW_OFFSET);

        assertThat(failed.getLastError()).isEqualTo("Kafka broker unavailable");

        assertThat(claimService.claimNextBatch()).isEmpty();
    }

    @Test
    void eventAtMaximumRetryCountShouldNotBeClaimed() {

        OutboxEventEntity event = newEvent();

        for (int attempt = 0; attempt < 5; attempt++) {
            String owner = "owner-" + attempt;

            event.claim(owner, NOW_OFFSET.minusSeconds(2), NOW_OFFSET.minusSeconds(1));

            boolean failed = event.markFailed(owner, NOW_OFFSET.minusSeconds(1), "Kafka failure");

            assertThat(failed).isTrue();
        }

        repository.saveAndFlush(event);

        assertThat(event.getRetryCount()).isEqualTo(5);

        assertThat(claimService.claimNextBatch()).isEmpty();
    }

    private OutboxEventEntity saveAndClaim() {

        repository.saveAndFlush(newEvent());

        return claimService.claimNextBatch().getFirst();
    }

    private OutboxEventEntity newEvent() {

        UUID bookingId = UUID.randomUUID();

        return new OutboxEventEntity(
                UUID.randomUUID(),
                AggregateType.BOOKING,
                bookingId,
                "seat-reservation-requested",
                "1",
                "seat-reservation-requested",
                bookingId.toString(),
                NOW_OFFSET,
                UUID.randomUUID(),
                null,
                """
                {
                  "bookingId": "%s",
                  "seatNumbers": ["H7"]
                }
                """
                        .formatted(bookingId),
                NOW_OFFSET);
    }

    private static void await(CountDownLatch latch) {

        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for latch");
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new AssertionError("Interrupted while waiting for latch", exception);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {

        @Bean
        OutboxProperties outboxProperties() {

            return new OutboxProperties(
                    "booking-service",
                    100,
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(30),
                    5,
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(10),
                    Duration.ZERO);
        }

        @Bean
        @Primary
        Clock fixedClock() {

            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        RandomGenerator randomGenerator() {

            return new Random(0);
        }
    }
}
