package com.cinema.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

class BookingCancellationIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired private BookingCancellationService bookingCancellationService;

    @Autowired private BookingRepository bookingRepository;

    @Autowired private BookingSeatRepository bookingSeatRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {

        outboxRepository.deleteAll();
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @AfterEach
    void cleanDatabaseAfterTest() {

        outboxRepository.deleteAll();
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void cancellationShouldAtomicallyUpdateBookingAndCreateOutbox() throws Exception {

        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        Booking booking = persistReservedBooking(userId, showtimeId);

        BookingResponse response = bookingCancellationService.cancel(userId, booking.getId());

        Booking reloadedBooking = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(response.id()).isEqualTo(booking.getId());

        assertThat(response.userId()).isEqualTo(userId);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(response.cancelledAt()).isNotNull();

        assertThat(reloadedBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(reloadedBooking.getCancelledAt()).isNotNull();

        List<OutboxEventEntity> cancellationEvents =
                outboxRepository.findAll().stream()
                        .filter(
                                event ->
                                        BookingEventContract.BOOKING_CANCELLED.equals(
                                                event.getEventType()))
                        .toList();

        assertThat(cancellationEvents).hasSize(1);

        OutboxEventEntity event = cancellationEvents.getFirst();

        assertThat(event.getAggregateId()).isEqualTo(booking.getId());

        assertThat(event.getPartitionKey()).isEqualTo(booking.getId().toString());

        assertThat(event.getOccurredAt()).isEqualTo(reloadedBooking.getCancelledAt());

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.get("bookingId").asText()).isEqualTo(booking.getId().toString());

        assertThat(payload.get("userId").asText()).isEqualTo(userId.toString());

        assertThat(payload.get("showtimeId").asText()).isEqualTo(showtimeId.toString());

        assertThat(payload.get("reason").asText()).isEqualTo("USER_REQUESTED");

        assertThat(OffsetDateTime.parse(payload.get("cancelledAt").asText()))
                .isEqualTo(reloadedBooking.getCancelledAt());
    }

    @Test
    void foreignOwnerShouldNotCancelOrCreateOutbox() {

        UUID ownerId = UuidGenerator.next();

        Booking booking = persistReservedBooking(ownerId, UuidGenerator.next());

        assertThrows(
                NotFoundException.class,
                () -> bookingCancellationService.cancel(UuidGenerator.next(), booking.getId()));

        Booking reloadedBooking = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(reloadedBooking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(reloadedBooking.getCancelledAt()).isNull();

        assertThat(outboxRepository.findAll())
                .noneMatch(
                        event ->
                                BookingEventContract.BOOKING_CANCELLED.equals(
                                        event.getEventType()));
    }

    @Test
    void invalidStateShouldNotCreateCancellationOutbox() {

        OffsetDateTime now = OffsetDateTime.now();

        UUID userId = UuidGenerator.next();

        Booking pendingBooking =
                new Booking(
                        userId,
                        UuidGenerator.next(),
                        "pending-request",
                        "b".repeat(64),
                        now.plusMinutes(10),
                        now);

        bookingRepository.saveAndFlush(pendingBooking);

        assertThrows(
                ConflictException.class,
                () -> bookingCancellationService.cancel(userId, pendingBooking.getId()));

        Booking reloadedBooking = bookingRepository.findById(pendingBooking.getId()).orElseThrow();

        assertThat(reloadedBooking.getStatus()).isEqualTo(BookingStatus.PENDING);

        assertThat(outboxRepository.findAll())
                .noneMatch(
                        event ->
                                BookingEventContract.BOOKING_CANCELLED.equals(
                                        event.getEventType()));
    }

    private Booking persistReservedBooking(UUID userId, UUID showtimeId) {

        OffsetDateTime now = OffsetDateTime.now();

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "request-" + UuidGenerator.next(),
                        "a".repeat(64),
                        now.plusMinutes(10),
                        now);

        booking.reserve(new BigDecimal("250000.00"), "VND");

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        bookingSeatRepository.saveAndFlush(new BookingSeat(savedBooking.getId(), showtimeId, "H7"));

        return savedBooking;
    }
}
