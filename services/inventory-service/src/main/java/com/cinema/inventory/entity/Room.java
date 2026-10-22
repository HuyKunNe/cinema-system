package com.cinema.inventory.entity;

import com.cinema.common.api.screening.ScreeningFormat;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rooms")
public class Room extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 50)
    private RoomType roomType;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private Set<Seat> seats = new HashSet<>();

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private Set<Showtime> showtimes = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "room_screening_formats", joinColumns = @JoinColumn(name = "room_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "screening_format", nullable = false, length = 40)
    private Set<ScreeningFormat> screeningFormats = new HashSet<>();

    protected Room() {}

    public Room(Cinema cinema, String name, RoomType roomType) {

        this.cinema = cinema;
        this.name = name;
        this.roomType = roomType;
        this.active = true;
    }

    public Cinema getCinema() {
        return cinema;
    }

    void assignCinema(Cinema cinema) {
        this.cinema = cinema;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public boolean isActive() {
        return active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Set<Seat> getSeats() {
        return Set.copyOf(seats);
    }

    public Set<Showtime> getShowtimes() {
        return Set.copyOf(showtimes);
    }

    public void addSeat(Seat seat) {
        if (seat == null) {
            return;
        }

        seats.add(seat);
        seat.assignRoom(this);
    }

    public void addShowtime(Showtime showtime) {
        if (showtime == null) {
            return;
        }

        showtimes.add(showtime);
        showtime.assignRoom(this);
    }

    public Set<ScreeningFormat> getScreeningFormats() {
        return Set.copyOf(screeningFormats);
    }

    public void replaceScreeningFormats(Set<ScreeningFormat> formats) {
        if (formats == null || formats.stream().anyMatch(java.util.Objects::isNull)) {
            throw new ValidationException(InventoryErrorCode.ROOM_SCREENING_FORMATS_INVALID);
        }

        Set<ScreeningFormat> replacement = new HashSet<>(formats);

        screeningFormats.clear();
        screeningFormats.addAll(replacement);
    }
}
