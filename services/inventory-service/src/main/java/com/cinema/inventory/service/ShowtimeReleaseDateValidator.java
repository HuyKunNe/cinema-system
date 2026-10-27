package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.exception.InventoryErrorCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class ShowtimeReleaseDateValidator {

    private ShowtimeReleaseDateValidator() {}

    public static void validate(
            OffsetDateTime startsAt, String configuredTimeZone, LocalDate releaseDate) {

        if (releaseDate == null) {
            return;
        }

        if (startsAt == null) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_PERIOD_REQUIRED);
        }

        String zoneName = configuredTimeZone == null ? "Asia/Ho_Chi_Minh" : configuredTimeZone;

        if (!ZoneId.getAvailableZoneIds().contains(zoneName)) {
            throw new ValidationException(InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }

        ZoneId cinemaZone = ZoneId.of(zoneName);

        LocalDate screeningDate = startsAt.atZoneSameInstant(cinemaZone).toLocalDate();

        if (screeningDate.isBefore(releaseDate)) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_BEFORE_RELEASE_DATE);
        }
    }
}
