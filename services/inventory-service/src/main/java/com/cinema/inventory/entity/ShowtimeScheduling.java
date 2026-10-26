package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.OffsetDateTime;

@Embeddable
public class ShowtimeScheduling {

    @Column(name = "schedule_movie_revision", updatable = false)
    private Long movieRevision;

    @Column(name = "movie_duration_minutes", updatable = false)
    private Integer durationMinutes;

    @Column(name = "break_minutes")
    private Integer breakMinutes;

    @Column(name = "ready_at")
    private OffsetDateTime readyAt;

    protected ShowtimeScheduling() {}

    public ShowtimeScheduling(
            Long movieRevision,
            Integer durationMinutes,
            Integer breakMinutes,
            OffsetDateTime endsAt) {

        if (movieRevision == null
                || movieRevision < 0
                || durationMinutes == null
                || durationMinutes < 1
                || endsAt == null) {
            throw new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_UPDATE);
        }

        validateBreak(breakMinutes);

        this.movieRevision = movieRevision;
        this.durationMinutes = durationMinutes;
        this.breakMinutes = breakMinutes;
        this.readyAt = endsAt.plusMinutes(breakMinutes);
    }

    public ShowtimeScheduling rescheduled(OffsetDateTime endsAt, Integer breakMinutes) {

        return new ShowtimeScheduling(movieRevision, durationMinutes, breakMinutes, endsAt);
    }

    public void validateDuration(OffsetDateTime startsAt, OffsetDateTime endsAt) {

        if (startsAt == null
                || endsAt == null
                || !startsAt.plusMinutes(durationMinutes).isEqual(endsAt)) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_DURATION_MISMATCH);
        }
    }

    public static void validateBreak(Integer breakMinutes) {
        if (breakMinutes == null || breakMinutes < 0 || breakMinutes > 120) {
            throw new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_BREAK);
        }
    }

    public Long getMovieRevision() {
        return movieRevision;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public Integer getBreakMinutes() {
        return breakMinutes;
    }

    public OffsetDateTime getReadyAt() {
        return readyAt;
    }
}
