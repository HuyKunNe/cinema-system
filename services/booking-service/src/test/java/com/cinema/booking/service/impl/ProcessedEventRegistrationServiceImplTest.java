package com.cinema.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.ProcessedEventRepository;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ProcessedEventRegistrationServiceImplTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-24T10:00:00Z");

    @Mock private ProcessedEventRepository processedEventRepository;

    private ProcessedEventRegistrationServiceImpl service;

    @BeforeEach
    void setUp() {

        Clock clock = Clock.fixed(NOW.toInstant(), ZoneOffset.UTC);

        service = new ProcessedEventRegistrationServiceImpl(processedEventRepository, clock);
    }

    @Test
    void newEventShouldBeRegistered() {

        UUID eventId = UuidGenerator.next();

        when(processedEventRepository.insertIfAbsent(
                        any(String.class),
                        eq(eventId.toString()),
                        eq("booking-seat-reserved"),
                        eq("seat-reserved"),
                        eq("1"),
                        eq(NOW)))
                .thenReturn(1);

        boolean result =
                service.register(eventId, " booking-seat-reserved ", " seat-reserved ", " 1 ");

        assertThat(result).isTrue();

        verify(processedEventRepository)
                .insertIfAbsent(
                        any(String.class),
                        eq(eventId.toString()),
                        eq("booking-seat-reserved"),
                        eq("seat-reserved"),
                        eq("1"),
                        eq(NOW));
    }

    @Test
    void duplicateEventShouldReturnFalse() {

        UUID eventId = UuidGenerator.next();

        when(processedEventRepository.insertIfAbsent(
                        any(String.class),
                        eq(eventId.toString()),
                        eq("booking-seat-reserved"),
                        eq("seat-reserved"),
                        eq("1"),
                        eq(NOW)))
                .thenReturn(0);

        boolean result = service.register(eventId, "booking-seat-reserved", "seat-reserved", "1");

        assertThat(result).isFalse();
    }

    @Test
    void missingEventIdShouldBeRejected() {

        assertThatThrownBy(
                        () -> service.register(null, "booking-seat-reserved", "seat-reserved", "1"))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.EVENT_ID_REQUIRED));

        verifyNoInteractions(processedEventRepository);
    }

    @Test
    void blankConsumerNameShouldBeRejected() {

        assertThatThrownBy(() -> service.register(UuidGenerator.next(), " ", "seat-reserved", "1"))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(processedEventRepository);
    }

    @Test
    void blankEventTypeShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                service.register(
                                        UuidGenerator.next(), "booking-seat-reserved", null, "1"))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(processedEventRepository);
    }

    @Test
    void blankEventVersionShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                service.register(
                                        UuidGenerator.next(),
                                        "booking-seat-reserved",
                                        "seat-reserved",
                                        " "))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(processedEventRepository);
    }
}
