package com.cinema.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.outbox.service.OutboxService;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class BookingExpirationIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired private BookingExpirationService bookingExpirationService;

    @Autowired private BookingCancellationService bookingCancellationService;

    @Autowired private BookingRepository bookingRepository;

    @Autowired private BookingSeatRepository bookingSeatRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    @MockitoSpyBean private OutboxService outboxService;

    @BeforeEach
    void cleanDatabase() {

        reset(outboxService);

        outboxRepository.deleteAll();
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @AfterEach
    void cleanDatabaseAfterTest() {

        reset(outboxService);

        outboxRepository.deleteAll();
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void expirationShouldAtomicallyUpdateBookingAndCreateOutbox() throws Exception {

        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        Booking booking = persistExpiredReservedBooking(userId, showtimeId);

        boolean expired = bookingExpirationService.expireIfDue(booking.getId());

        assertThat(expired).isTrue();

        Booking reloadedBooking = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(reloadedBooking.getStatus()).isEqualTo(BookingStatus.EXPIRED);

        assertThat(reloadedBooking.getCancelledAt()).isNull();

        List<OutboxEventEntity> expirationEvents =
                lifecycleEvents(BookingEventContract.BOOKING_EXPIRED);

        assertThat(expirationEvents).hasSize(1);

        OutboxEventEntity event = expirationEvents.getFirst();

        assertThat(event.getAggregateId()).isEqualTo(booking.getId());

        assertThat(event.getEventVersion()).isEqualTo(BookingEventContract.BOOKING_EXPIRED_VERSION);

        assertThat(event.getTopic()).isEqualTo(BookingEventContract.BOOKING_EXPIRED);

        assertThat(event.getPartitionKey()).isEqualTo(booking.getId().toString());

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.get("bookingId").asText()).isEqualTo(booking.getId().toString());

        assertThat(payload.get("userId").asText()).isEqualTo(userId.toString());

        assertThat(payload.get("showtimeId").asText()).isEqualTo(showtimeId.toString());

        assertThat(OffsetDateTime.parse(payload.get("expiredAt").asText()))
                .isEqualTo(event.getOccurredAt());
    }

    @Test
    void outboxFailureShouldRollbackBookingExpiration() {

        Booking booking = persistExpiredReservedBooking(UuidGenerator.next(), UuidGenerator.next());

        doThrow(new IllegalStateException("Simulated Outbox persistence failure"))
                .when(outboxService)
                .save(any(OutboxEventEntity.class));

        assertThrows(
                IllegalStateException.class,
                () -> bookingExpirationService.expireIfDue(booking.getId()));

        Booking reloadedBooking = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(reloadedBooking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(reloadedBooking.getCancelledAt()).isNull();

        assertThat(lifecycleEvents(BookingEventContract.BOOKING_EXPIRED)).isEmpty();
    }

    @Test
    void concurrentExpirationShouldCreateExactlyOneOutboxEvent() throws Exception {

        Booking booking = persistExpiredReservedBooking(UuidGenerator.next(), UuidGenerator.next());

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> first =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();

                                return bookingExpirationService.expireIfDue(booking.getId());
                            });

            Future<Boolean> second =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();

                                return bookingExpirationService.expireIfDue(booking.getId());
                            });

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            List<Boolean> results =
                    List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));

            assertThat(results).containsExactlyInAnyOrder(true, false);

        } finally {
            executor.shutdownNow();
        }

        Booking reloadedBooking = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(reloadedBooking.getStatus()).isEqualTo(BookingStatus.EXPIRED);

        assertThat(lifecycleEvents(BookingEventContract.BOOKING_EXPIRED)).hasSize(1);

        assertThat(lifecycleEvents(BookingEventContract.BOOKING_CANCELLED)).isEmpty();
    }

    @Test
    void expirationShouldWinCancellationRaceAtOrAfterExpiresAt() throws Exception {

        UUID userId = UuidGenerator.next();

        Booking booking = persistExpiredReservedBooking(userId, UuidGenerator.next());

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> expiration =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();

                                return bookingExpirationService.expireIfDue(booking.getId());
                            });

            Future<Boolean> cancellation =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();

                                try {
                                    bookingCancellationService.cancel(userId, booking.getId());

                                    return true;

                                } catch (ConflictException exception) {
                                    return false;
                                }
                            });

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            assertThat(expiration.get(30, TimeUnit.SECONDS)).isTrue();

            assertThat(cancellation.get(30, TimeUnit.SECONDS)).isFalse();

        } finally {
            executor.shutdownNow();
        }

        Booking reloadedBooking = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(reloadedBooking.getStatus()).isEqualTo(BookingStatus.EXPIRED);

        assertThat(reloadedBooking.getCancelledAt()).isNull();

        assertThat(lifecycleEvents(BookingEventContract.BOOKING_EXPIRED)).hasSize(1);

        assertThat(lifecycleEvents(BookingEventContract.BOOKING_CANCELLED)).isEmpty();
    }

    @Test
    void futureReservedBookingShouldNotExpireOrCreateOutbox() {

        Booking booking = persistFutureReservedBooking(UuidGenerator.next(), UuidGenerator.next());

        boolean expired = bookingExpirationService.expireIfDue(booking.getId());

        assertThat(expired).isFalse();

        Booking reloadedBooking = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(reloadedBooking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(lifecycleEvents(BookingEventContract.BOOKING_EXPIRED)).isEmpty();
    }

    private Booking persistExpiredReservedBooking(UUID userId, UUID showtimeId) {

        OffsetDateTime now = OffsetDateTime.now();

        return persistReservedBooking(
                userId, showtimeId, now.minusSeconds(1), now.minusMinutes(10));
    }

    private Booking persistFutureReservedBooking(UUID userId, UUID showtimeId) {

        OffsetDateTime now = OffsetDateTime.now();

        return persistReservedBooking(userId, showtimeId, now.plusMinutes(10), now);
    }

    private Booking persistReservedBooking(
            UUID userId, UUID showtimeId, OffsetDateTime expiresAt, OffsetDateTime createdAt) {

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "expiration-" + UuidGenerator.next(),
                        "a".repeat(64),
                        expiresAt,
                        createdAt);

        booking.reserve(new BigDecimal("250000.00"), "VND");

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        bookingSeatRepository.saveAndFlush(new BookingSeat(savedBooking.getId(), showtimeId, "H7"));

        return savedBooking;
    }

    private List<OutboxEventEntity> lifecycleEvents(String eventType) {

        return outboxRepository.findAll().stream()
                .filter(event -> eventType.equals(event.getEventType()))
                .toList();
    }
}
