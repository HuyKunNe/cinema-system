package com.cinema.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.SeatReleaseRequestedPayload;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.ProcessedEventRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.SeatReleaseRequestedConsumerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Import(SeatReleaseRequestedConsumerMySqlIntegrationTest.FixedClockConfiguration.class)
class SeatReleaseRequestedConsumerMySqlIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final Instant CURRENT_INSTANT = Instant.parse("2026-09-15T10:01:00Z");

    private static final OffsetDateTime RELEASED_AT =
            OffsetDateTime.ofInstant(CURRENT_INSTANT, ZoneOffset.UTC);

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-15T10:00:00Z");

    private static final OffsetDateTime HOLD_EXPIRES_AT =
            OffsetDateTime.parse("2026-09-15T10:10:00Z");

    @Autowired private SeatReleaseRequestedConsumerService consumerService;

    @Autowired private CinemaRepository cinemaRepository;

    @Autowired private RoomRepository roomRepository;

    @Autowired private SeatRepository seatRepository;

    @Autowired private ShowtimeRepository showtimeRepository;

    @Autowired private ShowSeatRepository showSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EntityManager entityManager;

    private ExecutorService executorService;

    private TestContext context;

    @BeforeEach
    void setUp() {

        cleanDatabase();

        executorService = Executors.newFixedThreadPool(2);

        context = createHeldInventory();
    }

    @AfterEach
    void tearDown() throws InterruptedException {

        executorService.shutdown();

        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }

        cleanDatabase();
    }

    @Test
    void canonicalRequestShouldReleaseCompleteSeatSetAndCreateOutbox() throws Exception {

        OutboxEventMessage message =
                releaseMessage(UuidGenerator.next(), context.bookingId(), reversedSeatIds());

        SeatReleaseRequestedConsumerService.Result result =
                consumerService.handle(context.bookingId().toString(), message);

        entityManager.clear();

        List<ShowSeat> showSeats = loadShowSeats();

        assertThat(result.status()).isEqualTo(SeatReleaseRequestedConsumerService.Status.RELEASED);

        assertThat(result.outboxEventId()).isNotNull();

        assertThat(showSeats).extracting(ShowSeat::getSeatNumber).containsExactly("H7", "H8");

        assertThat(showSeats)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(),
                                InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER))
                .isTrue();

        assertThat(outboxRepository.findAll())
                .singleElement()
                .satisfies(
                        outboxEvent -> {
                            assertThat(outboxEvent.getId()).isEqualTo(result.outboxEventId());

                            assertCanonicalOutbox(outboxEvent, message);
                        });
    }

    @Test
    void duplicateRequestShouldReleaseAndCreateOutboxOnlyOnce() {

        OutboxEventMessage message =
                releaseMessage(UuidGenerator.next(), context.bookingId(), context.showSeatIds());

        SeatReleaseRequestedConsumerService.Result first =
                consumerService.handle(context.bookingId().toString(), message);

        SeatReleaseRequestedConsumerService.Result duplicate =
                consumerService.handle(context.bookingId().toString(), message);

        entityManager.clear();

        assertThat(first.status()).isEqualTo(SeatReleaseRequestedConsumerService.Status.RELEASED);

        assertThat(first.outboxEventId()).isNotNull();

        assertThat(duplicate.status())
                .isEqualTo(SeatReleaseRequestedConsumerService.Status.DUPLICATE);

        assertThat(duplicate.outboxEventId()).isNull();

        assertThat(loadShowSeats())
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    @Test
    void missingRequestedSeatShouldRollbackMarkerAndKeepEverySeatHeld() {

        UUID missingSeatId = UuidGenerator.next();

        OutboxEventMessage message =
                releaseMessage(
                        UuidGenerator.next(),
                        context.bookingId(),
                        List.of(context.showSeatIds().get(0), missingSeatId));

        assertThatThrownBy(() -> consumerService.handle(context.bookingId().toString(), message))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        assertStillHeldByOriginalBooking();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(),
                                InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER))
                .isFalse();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void distinctDelayedRequestShouldNotReleaseAgain() {

        OutboxEventMessage firstMessage =
                releaseMessage(UuidGenerator.next(), context.bookingId(), context.showSeatIds());

        OutboxEventMessage delayedMessage =
                releaseMessage(UuidGenerator.next(), context.bookingId(), reversedSeatIds());

        SeatReleaseRequestedConsumerService.Result first =
                consumerService.handle(context.bookingId().toString(), firstMessage);

        assertThatThrownBy(
                        () ->
                                consumerService.handle(
                                        context.bookingId().toString(), delayedMessage))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        assertThat(first.status()).isEqualTo(SeatReleaseRequestedConsumerService.Status.RELEASED);

        assertThat(loadShowSeats())
                .allSatisfy(
                        showSeat ->
                                assertThat(showSeat.getStatus())
                                        .isEqualTo(ShowSeatStatus.AVAILABLE));

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                firstMessage.eventId(),
                                InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER))
                .isTrue();

        /*
         * Marker của event bị từ chối phải rollback cùng transaction.
         */
        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                delayedMessage.eventId(),
                                InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER))
                .isFalse();

        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    @Test
    void concurrentDuplicateRequestsShouldReleaseOnce() throws Exception {

        OutboxEventMessage message =
                releaseMessage(UuidGenerator.next(), context.bookingId(), reversedSeatIds());

        List<ConcurrentOutcome> outcomes = executeConcurrently(message, message);

        assertThat(outcomes).allSatisfy(outcome -> assertThat(outcome.failure()).isNull());

        assertThat(outcomes)
                .extracting(ConcurrentOutcome::status)
                .containsExactlyInAnyOrder(
                        SeatReleaseRequestedConsumerService.Status.RELEASED,
                        SeatReleaseRequestedConsumerService.Status.DUPLICATE);

        entityManager.clear();

        assertThat(loadShowSeats())
                .allSatisfy(
                        showSeat ->
                                assertThat(showSeat.getStatus())
                                        .isEqualTo(ShowSeatStatus.AVAILABLE));

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    @Test
    void concurrentDistinctRequestsShouldHaveOneReleaseWinner() throws Exception {

        OutboxEventMessage firstMessage =
                releaseMessage(UuidGenerator.next(), context.bookingId(), context.showSeatIds());

        OutboxEventMessage secondMessage =
                releaseMessage(UuidGenerator.next(), context.bookingId(), reversedSeatIds());

        List<ConcurrentOutcome> outcomes = executeConcurrently(firstMessage, secondMessage);

        assertThat(outcomes)
                .filteredOn(outcome -> outcome.failure() == null)
                .extracting(ConcurrentOutcome::status)
                .containsExactly(SeatReleaseRequestedConsumerService.Status.RELEASED);

        assertThat(outcomes)
                .filteredOn(outcome -> outcome.failure() != null)
                .singleElement()
                .satisfies(
                        outcome ->
                                assertThat(outcome.failure())
                                        .isInstanceOf(ConflictException.class));

        entityManager.clear();

        assertThat(loadShowSeats())
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        List.of(
                                processedEventRepository.existsByEventIdAndConsumerName(
                                        firstMessage.eventId(),
                                        InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER),
                                processedEventRepository.existsByEventIdAndConsumerName(
                                        secondMessage.eventId(),
                                        InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER)))
                .containsExactlyInAnyOrder(true, false);

        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    private List<ConcurrentOutcome> executeConcurrently(
            OutboxEventMessage firstMessage, OutboxEventMessage secondMessage) throws Exception {

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        Future<ConcurrentOutcome> first =
                executorService.submit(() -> handleConcurrently(firstMessage, ready, start));

        Future<ConcurrentOutcome> second =
                executorService.submit(() -> handleConcurrently(secondMessage, ready, start));

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

        start.countDown();

        return List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
    }

    private ConcurrentOutcome handleConcurrently(
            OutboxEventMessage message, CountDownLatch ready, CountDownLatch start) {

        try {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
                return ConcurrentOutcome.failure(
                        new IllegalStateException("Timed out waiting to start seat release"));
            }

            SeatReleaseRequestedConsumerService.Result result =
                    consumerService.handle(context.bookingId().toString(), message);

            return ConcurrentOutcome.success(result.status());

        } catch (Throwable failure) {
            return ConcurrentOutcome.failure(failure);
        }
    }

    private TestContext createHeldInventory() {

        UUID bookingId = UuidGenerator.next();

        Cinema cinema =
                cinemaRepository.saveAndFlush(
                        new Cinema(
                                "Seat Release MySQL Test", "123 Main Street", "Ho Chi Minh City"));

        Room room =
                roomRepository.saveAndFlush(
                        new Room(cinema, "Release Integration Room", RoomType.STANDARD));

        Seat h7 = seatRepository.saveAndFlush(new Seat(room, "H7", "H", SeatType.STANDARD));

        Seat h8 = seatRepository.saveAndFlush(new Seat(room, "H8", "H", SeatType.VIP));

        Showtime showtime =
                new Showtime(
                        UuidGenerator.next(),
                        room,
                        RELEASED_AT.plusDays(1),
                        RELEASED_AT.plusDays(1).plusHours(2));

        showtime.openForBooking();

        Showtime savedShowtime = showtimeRepository.saveAndFlush(showtime);

        ShowSeat firstShowSeat = new ShowSeat(savedShowtime, h7, new BigDecimal("90000.00"));

        firstShowSeat.hold(bookingId, HOLD_EXPIRES_AT, REQUESTED_AT.minusMinutes(1));

        ShowSeat secondShowSeat = new ShowSeat(savedShowtime, h8, new BigDecimal("120000.00"));

        secondShowSeat.hold(bookingId, HOLD_EXPIRES_AT, REQUESTED_AT.minusMinutes(1));

        List<ShowSeat> savedShowSeats =
                showSeatRepository.saveAllAndFlush(List.of(firstShowSeat, secondShowSeat));

        List<UUID> sortedShowSeatIds =
                savedShowSeats.stream()
                        .map(ShowSeat::getId)
                        .sorted(Comparator.naturalOrder())
                        .toList();

        entityManager.clear();

        return new TestContext(bookingId, savedShowtime.getId(), sortedShowSeatIds);
    }

    private OutboxEventMessage releaseMessage(UUID eventId, UUID bookingId, List<UUID> seatIds) {

        SeatReleaseRequestedPayload payload =
                new SeatReleaseRequestedPayload(
                        bookingId,
                        context.showtimeId(),
                        seatIds,
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        REQUESTED_AT);

        return new OutboxEventMessage(
                eventId,
                bookingId,
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.SEAT_RELEASE_REQUESTED,
                InventoryEventContract.SEAT_RELEASE_REQUESTED_VERSION,
                REQUESTED_AT,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private void assertCanonicalOutbox(
            OutboxEventEntity outboxEvent, OutboxEventMessage sourceMessage) {

        assertThat(outboxEvent.getEventType()).isEqualTo(InventoryEventContract.SEAT_RELEASED);

        assertThat(outboxEvent.getEventVersion())
                .isEqualTo(InventoryEventContract.SEAT_RELEASED_VERSION);

        assertThat(outboxEvent.getTopic()).isEqualTo(InventoryEventContract.SEAT_RELEASED);

        assertThat(outboxEvent.getAggregateId()).isEqualTo(context.bookingId());

        assertThat(outboxEvent.getPartitionKey()).isEqualTo(context.bookingId().toString());

        assertThat(outboxEvent.getOccurredAt()).isEqualTo(RELEASED_AT);

        assertThat(outboxEvent.getCorrelationId()).isEqualTo(sourceMessage.correlationId());

        assertThat(outboxEvent.getCausationId()).isEqualTo(sourceMessage.eventId());

        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);

        try {
            JsonNode payload = objectMapper.readTree(outboxEvent.getPayload());

            assertThat(payload.path("bookingId").asText())
                    .isEqualTo(context.bookingId().toString());

            assertThat(payload.path("showtimeId").asText())
                    .isEqualTo(context.showtimeId().toString());

            assertThat(payload.path("reason").asText())
                    .isEqualTo(InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED);

            assertThat(OffsetDateTime.parse(payload.path("releasedAt").asText()))
                    .isEqualTo(RELEASED_AT);

            assertThat(payload.path("releasedSeatIds"))
                    .extracting(JsonNode::asText)
                    .containsExactly(
                            context.showSeatIds().get(0).toString(),
                            context.showSeatIds().get(1).toString());

        } catch (Exception exception) {
            throw new AssertionError("Unable to verify seat-released payload", exception);
        }
    }

    private List<UUID> reversedSeatIds() {

        return List.of(context.showSeatIds().get(1), context.showSeatIds().get(0));
    }

    private List<ShowSeat> loadShowSeats() {

        return showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(context.showtimeId());
    }

    private void assertStillHeldByOriginalBooking() {

        assertThat(loadShowSeats())
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD);

                            assertThat(showSeat.getHeldByBookingId())
                                    .isEqualTo(context.bookingId());

                            assertThat(showSeat.getHoldExpiresAt()).isEqualTo(HOLD_EXPIRES_AT);
                        });
    }

    private void cleanDatabase() {

        outboxRepository.deleteAllInBatch();

        processedEventRepository.deleteAllInBatch();

        showSeatRepository.deleteAllInBatch();

        showtimeRepository.deleteAllInBatch();

        seatRepository.deleteAllInBatch();

        roomRepository.deleteAllInBatch();

        cinemaRepository.deleteAllInBatch();
    }

    private record TestContext(UUID bookingId, UUID showtimeId, List<UUID> showSeatIds) {}

    private record ConcurrentOutcome(
            SeatReleaseRequestedConsumerService.Status status, Throwable failure) {

        private static ConcurrentOutcome success(
                SeatReleaseRequestedConsumerService.Status status) {

            return new ConcurrentOutcome(status, null);
        }

        private static ConcurrentOutcome failure(Throwable failure) {

            return new ConcurrentOutcome(null, failure);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedSeatReleaseIntegrationClock() {

            return Clock.fixed(CURRENT_INSTANT, ZoneOffset.UTC);
        }
    }
}
