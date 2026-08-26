package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.repository.ProcessedEventRepository;

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

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse("2026-08-26T10:00:00Z");

    private static final String CONSUMER_NAME =
            "payment-request-processing";

    private static final String EVENT_TYPE =
            "payment-requested";

    private static final String EVENT_VERSION = "1";

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private ProcessedEventRegistrationServiceImpl service;

    @BeforeEach
    void setUp() {

        Clock clock =
                Clock.fixed(
                        NOW.toInstant(),
                        ZoneOffset.UTC);

        service =
                new ProcessedEventRegistrationServiceImpl(
                        processedEventRepository,
                        clock);
    }

    @Test
    void newEventShouldBeRegistered() {

        UUID eventId = UuidGenerator.next();

        when(processedEventRepository.insertIfAbsent(
                        any(String.class),
                        eq(eventId.toString()),
                        eq(CONSUMER_NAME),
                        eq(EVENT_TYPE),
                        eq(EVENT_VERSION),
                        eq(NOW)))
                .thenReturn(1);

        boolean result =
                service.register(
                        eventId,
                        " payment-request-processing ",
                        " payment-requested ",
                        " 1 ");

        assertThat(result).isTrue();

        verify(processedEventRepository)
                .insertIfAbsent(
                        any(String.class),
                        eq(eventId.toString()),
                        eq(CONSUMER_NAME),
                        eq(EVENT_TYPE),
                        eq(EVENT_VERSION),
                        eq(NOW));
    }

    @Test
    void duplicateEventShouldReturnFalse() {

        UUID eventId = UuidGenerator.next();

        when(processedEventRepository.insertIfAbsent(
                        any(String.class),
                        eq(eventId.toString()),
                        eq(CONSUMER_NAME),
                        eq(EVENT_TYPE),
                        eq(EVENT_VERSION),
                        eq(NOW)))
                .thenReturn(0);

        boolean result =
                service.register(
                        eventId,
                        CONSUMER_NAME,
                        EVENT_TYPE,
                        EVENT_VERSION);

        assertThat(result).isFalse();

        verify(processedEventRepository)
                .insertIfAbsent(
                        any(String.class),
                        eq(eventId.toString()),
                        eq(CONSUMER_NAME),
                        eq(EVENT_TYPE),
                        eq(EVENT_VERSION),
                        eq(NOW));
    }

    @Test
    void missingEventIdShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                service.register(
                                        null,
                                        CONSUMER_NAME,
                                        EVENT_TYPE,
                                        EVENT_VERSION))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(
                                                ((ValidationException) throwable)
                                                        .getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .EVENT_ID_REQUIRED));

        verifyNoInteractions(processedEventRepository);
    }

    @Test
    void blankConsumerNameShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                service.register(
                                        UuidGenerator.next(),
                                        " ",
                                        EVENT_TYPE,
                                        EVENT_VERSION))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(
                                                ((ValidationException) throwable)
                                                        .getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .CONSUMER_NAME_REQUIRED));

        verifyNoInteractions(processedEventRepository);
    }

    @Test
    void blankEventTypeShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                service.register(
                                        UuidGenerator.next(),
                                        CONSUMER_NAME,
                                        null,
                                        EVENT_VERSION))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(
                                                ((ValidationException) throwable)
                                                        .getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .EVENT_TYPE_REQUIRED));

        verifyNoInteractions(processedEventRepository);
    }

    @Test
    void blankEventVersionShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                service.register(
                                        UuidGenerator.next(),
                                        CONSUMER_NAME,
                                        EVENT_TYPE,
                                        " "))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(
                                                ((ValidationException) throwable)
                                                        .getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .EVENT_VERSION_REQUIRED));

        verifyNoInteractions(processedEventRepository);
    }
}
