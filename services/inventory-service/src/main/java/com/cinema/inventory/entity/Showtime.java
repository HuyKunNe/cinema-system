package com.cinema.inventory.entity;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.ShowtimeStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "showtimes")
public class Showtime extends BaseEntity {

    @Column(name = "movie_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID movieId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ShowtimeStatus status = ShowtimeStatus.SCHEDULED;

    @OneToMany(mappedBy = "showtime", fetch = FetchType.LAZY)
    private Set<ShowSeat> showSeats = new HashSet<>();

    protected Showtime() {
    }

    public Showtime(
            UUID movieId,
            Room room,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt) {

        validateTimeRange(startsAt, endsAt);

        this.movieId = movieId;
        this.room = room;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = ShowtimeStatus.SCHEDULED;
    }

    public UUID getMovieId() {
        return movieId;
    }

    public void setMovieId(UUID movieId) {
        this.movieId = movieId;
    }

    public Room getRoom() {
        return room;
    }

    void assignRoom(Room room) {
        this.room = room;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public ShowtimeStatus getStatus() {
        return status;
    }

    public Set<ShowSeat> getShowSeats() {
        return Set.copyOf(showSeats);
    }

    public void changeSchedule(
            OffsetDateTime startsAt,
            OffsetDateTime endsAt) {

        validateTimeRange(startsAt, endsAt);

        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public void openForBooking() {
        requireStatus(ShowtimeStatus.SCHEDULED);
        this.status = ShowtimeStatus.OPEN_FOR_BOOKING;
    }

    public void close() {
        requireStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        this.status = ShowtimeStatus.CLOSED;
    }

    public void cancel() {
        if (status == ShowtimeStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Completed showtime cannot be cancelled");
        }

        this.status = ShowtimeStatus.CANCELLED;
    }

    public void complete() {
        if (status != ShowtimeStatus.CLOSED) {
            throw new IllegalStateException(
                    "Only a closed showtime can be completed");
        }

        this.status = ShowtimeStatus.COMPLETED;
    }

    public void addShowSeat(ShowSeat showSeat) {
        if (showSeat == null) {
            return;
        }

        showSeats.add(showSeat);
        showSeat.assignShowtime(this);
    }

    private void requireStatus(ShowtimeStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException(
                    "Expected showtime status "
                            + expectedStatus
                            + " but was "
                            + status);
        }
    }

    private static void validateTimeRange(
            OffsetDateTime startsAt,
            OffsetDateTime endsAt) {

        if (startsAt == null || endsAt == null) {
            throw new IllegalArgumentException(
                    "Showtime start and end are required");
        }

        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException(
                    "Showtime end must be after start");
        }
    }
}
