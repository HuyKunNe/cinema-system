package com.cinema.booking.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.service.BookingExpirationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

class BookingExpirationSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

    private static final OffsetDateTime NOW_OFFSET = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

    @Mock private BookingRepository bookingRepository;

    @Mock private BookingExpirationService expirationService;

    private BookingExpirationScheduler scheduler;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        scheduler =
                new BookingExpirationScheduler(
                        bookingRepository, expirationService, Clock.fixed(NOW, ZoneOffset.UTC), 25);
    }

    @Test
    void shouldProcessExpirationCandidates() {

        UUID firstBookingId = UUID.randomUUID();
        UUID secondBookingId = UUID.randomUUID();

        when(bookingRepository.findExpirationCandidateIds(
                        eq(BookingStatus.RESERVED), eq(NOW_OFFSET), any(Pageable.class)))
                .thenReturn(List.of(firstBookingId, secondBookingId));

        scheduler.expireDueBookings();

        verify(expirationService).expireIfDue(firstBookingId);

        verify(expirationService).expireIfDue(secondBookingId);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(bookingRepository)
                .findExpirationCandidateIds(
                        eq(BookingStatus.RESERVED), eq(NOW_OFFSET), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();

        assertThat(pageable.getPageSize()).isEqualTo(25);
    }

    @Test
    void oneFailureShouldNotStopRemainingCandidates() {

        UUID failedBookingId = UUID.randomUUID();
        UUID successfulBookingId = UUID.randomUUID();

        when(bookingRepository.findExpirationCandidateIds(
                        eq(BookingStatus.RESERVED), eq(NOW_OFFSET), any(Pageable.class)))
                .thenReturn(List.of(failedBookingId, successfulBookingId));

        when(expirationService.expireIfDue(failedBookingId))
                .thenThrow(new IllegalStateException("Simulated expiration failure"));

        scheduler.expireDueBookings();

        verify(expirationService).expireIfDue(failedBookingId);

        verify(expirationService).expireIfDue(successfulBookingId);
    }
}
