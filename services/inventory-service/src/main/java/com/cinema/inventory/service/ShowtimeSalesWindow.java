package com.cinema.inventory.service;

import com.cinema.inventory.entity.Showtime;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class ShowtimeSalesWindow {

    private static final String DEFAULT_CINEMA_TIME_ZONE = "Asia/Ho_Chi_Minh";

    public static final long OPEN_DAYS_BEFORE_SHOWTIME = 7;
    public static final long CLOSE_MINUTES_AFTER_SHOWTIME = 5;

    private ShowtimeSalesWindow() {}

    public static OffsetDateTime opensAt(Showtime showtime) {
        String configuredTimeZone = showtime.getRoom().getCinema().getTimeZone();
        ZoneId cinemaZone =
                ZoneId.of(
                        configuredTimeZone == null || configuredTimeZone.isBlank()
                                ? DEFAULT_CINEMA_TIME_ZONE
                                : configuredTimeZone);
        LocalDate showDate =
                showtime.getStartsAt().atZoneSameInstant(cinemaZone).toLocalDate();

        return showDate
                .minusDays(OPEN_DAYS_BEFORE_SHOWTIME)
                .atStartOfDay(cinemaZone)
                .toOffsetDateTime();
    }

    public static OffsetDateTime closesAt(Showtime showtime) {
        return showtime.getEndsAt().plusMinutes(CLOSE_MINUTES_AFTER_SHOWTIME);
    }

    public static boolean isOpenAt(Showtime showtime, OffsetDateTime now) {
        return !now.isBefore(opensAt(showtime)) && now.isBefore(closesAt(showtime));
    }
}
