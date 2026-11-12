package com.cinema.inventory.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.ShowtimeSalesWindow;
import com.cinema.inventory.service.ShowtimeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Component
public class ShowtimeSalesLifecycleScheduler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ShowtimeSalesLifecycleScheduler.class);

    private static final long OPENING_LOOKAHEAD_DAYS =
            ShowtimeSalesWindow.OPEN_DAYS_BEFORE_SHOWTIME + 1;

    private static final EnumSet<ShowtimeStatus> CLOSABLE_STATUSES =
            EnumSet.of(ShowtimeStatus.SCHEDULED, ShowtimeStatus.OPEN_FOR_BOOKING);

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeService showtimeService;
    private final Clock clock;

    public ShowtimeSalesLifecycleScheduler(
            ShowtimeRepository showtimeRepository, ShowtimeService showtimeService, Clock clock) {
        this.showtimeRepository = showtimeRepository;
        this.showtimeService = showtimeService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${cinema.inventory.showtime-sales.lifecycle-interval-ms:30000}")
    public void synchronizeSalesLifecycle() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        closeExpiredSales(now);
        openEligibleSales(now);
    }

    private void closeExpiredSales(OffsetDateTime now) {
        List<UUID> dueShowtimeIds =
                showtimeRepository.findIdsByStatusInAndEndsAtLessThanEqualOrderByEndsAtAscIdAsc(
                        CLOSABLE_STATUSES,
                        now.minusMinutes(ShowtimeSalesWindow.CLOSE_MINUTES_AFTER_SHOWTIME));

        for (UUID showtimeId : dueShowtimeIds) {
            try {
                showtimeService.closeSalesAutomatically(showtimeId);
            } catch (ConflictException exception) {
                LOGGER.debug(
                        "Automatic showtime close was skipped: showtimeId={}, reason={}",
                        showtimeId,
                        exception.getMessage());
            } catch (RuntimeException exception) {
                LOGGER.warn(
                        "Could not automatically close showtime sales: showtimeId={}",
                        showtimeId,
                        exception);
            }
        }
    }

    private void openEligibleSales(OffsetDateTime now) {
        List<Showtime> candidates =
                showtimeRepository.findScheduledForAutomaticOpening(
                        ShowtimeStatus.SCHEDULED,
                        now.plusDays(OPENING_LOOKAHEAD_DAYS),
                        now.minusMinutes(ShowtimeSalesWindow.CLOSE_MINUTES_AFTER_SHOWTIME));

        for (Showtime showtime : candidates) {
            try {
                if (!ShowtimeSalesWindow.isOpenAt(showtime, now)) {
                    continue;
                }

                showtimeService.openForBookingAutomatically(showtime.getId());
            } catch (ConflictException exception) {
                LOGGER.debug(
                        "Automatic showtime open was skipped: showtimeId={}, reason={}",
                        showtime.getId(),
                        exception.getMessage());
            } catch (RuntimeException exception) {
                LOGGER.warn(
                        "Could not automatically open showtime sales: showtimeId={}",
                        showtime.getId(),
                        exception);
            }
        }
    }
}
