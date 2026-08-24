package com.cinema.booking.scheduler;

import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.service.BookingExpirationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "cinema.booking.expiration",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class BookingExpirationScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingExpirationScheduler.class);

    private final BookingRepository bookingRepository;

    private final BookingExpirationService bookingExpirationService;

    private final Clock clock;

    private final int batchSize;

    public BookingExpirationScheduler(
            BookingRepository bookingRepository,
            BookingExpirationService bookingExpirationService,
            @Qualifier("systemClock") Clock clock,
            @Value("${cinema.booking.expiration.batch-size:100}") int batchSize) {

        this.bookingRepository = bookingRepository;
        this.bookingExpirationService = bookingExpirationService;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${cinema.booking.expiration.scheduler-delay:30s}")
    public void expireDueBookings() {

        OffsetDateTime cutoff = OffsetDateTime.now(clock);

        List<UUID> candidateIds =
                bookingRepository.findExpirationCandidateIds(
                        BookingStatus.RESERVED, cutoff, PageRequest.of(0, batchSize));

        candidateIds.forEach(this::expireCandidate);
    }

    private void expireCandidate(UUID bookingId) {

        try {
            bookingExpirationService.expireIfDue(bookingId);

        } catch (RuntimeException exception) {
            LOGGER.error("Failed to expire booking {}", bookingId, exception);
        }
    }
}
