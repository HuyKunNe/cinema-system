package com.cinema.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class BookingIdempotencyIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired private BookingService bookingService;

    @Autowired private BookingRepository bookingRepository;

    @Autowired private BookingSeatRepository bookingSeatRepository;

    @BeforeEach
    void cleanDatabase() {
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @AfterEach
    void cleanDatabaseAfterTest() {
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void sameRequestShouldReturnExistingBooking() {
        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        CreateBookingRequest request =
                new CreateBookingRequest("request-1", showtimeId, List.of("H7", "H8"));

        BookingResponse first = bookingService.create(userId, request);

        BookingResponse second = bookingService.create(userId, request);

        assertThat(second.id()).isEqualTo(first.id());

        assertThat(bookingRepository.count()).isEqualTo(1);

        assertThat(bookingSeatRepository.count()).isEqualTo(2);
    }

    @Test
    void differentSeatOrderShouldBeSameLogicalRequest() {
        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        BookingResponse first =
                bookingService.create(
                        userId,
                        new CreateBookingRequest("request-1", showtimeId, List.of("H7", "H8")));

        BookingResponse second =
                bookingService.create(
                        userId,
                        new CreateBookingRequest("request-1", showtimeId, List.of("H8", "H7")));

        assertThat(second.id()).isEqualTo(first.id());

        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void reusedRequestIdWithDifferentPayloadShouldConflict() {
        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        bookingService.create(
                userId, new CreateBookingRequest("request-1", showtimeId, List.of("H7")));

        assertThrows(
                ConflictException.class,
                () ->
                        bookingService.create(
                                userId,
                                new CreateBookingRequest("request-1", showtimeId, List.of("H8"))));

        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void sameRequestIdMayBeUsedByDifferentUsers() {
        UUID showtimeId = UuidGenerator.next();

        BookingResponse first =
                bookingService.create(
                        UuidGenerator.next(),
                        new CreateBookingRequest("request-1", showtimeId, List.of("H7")));

        BookingResponse second =
                bookingService.create(
                        UuidGenerator.next(),
                        new CreateBookingRequest("request-1", showtimeId, List.of("H7")));

        assertThat(second.id()).isNotEqualTo(first.id());

        assertThat(bookingRepository.count()).isEqualTo(2);
    }

    @Test
    void concurrentSameRequestShouldCreateOneBooking() throws Exception {

        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        CreateBookingRequest request =
                new CreateBookingRequest("concurrent-request-1", showtimeId, List.of("H7", "H8"));

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<BookingResponse> first =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();

                                return bookingService.create(userId, request);
                            });

            Future<BookingResponse> second =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();

                                return bookingService.create(userId, request);
                            });

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            BookingResponse firstResponse = first.get(30, TimeUnit.SECONDS);

            BookingResponse secondResponse = second.get(30, TimeUnit.SECONDS);

            assertThat(secondResponse.id()).isEqualTo(firstResponse.id());

            assertThat(bookingRepository.count()).isEqualTo(1);

            assertThat(bookingSeatRepository.count()).isEqualTo(2);

        } finally {
            executor.shutdownNow();
        }
    }
}
