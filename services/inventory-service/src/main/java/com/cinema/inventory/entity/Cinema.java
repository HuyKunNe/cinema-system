package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cinemas")
public class Cinema extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "time_zone", length = 64)
    private String timeZone;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "cinema", fetch = FetchType.LAZY)
    private Set<Room> rooms = new HashSet<>();

    protected Cinema() {}

    public Cinema(String name, String address, String city) {

        this.name = name;
        this.address = address;
        this.city = city;
        this.active = true;
    }

    public void update(String name, String address, String city) {
        this.name = name;
        this.address = address;
        this.city = city;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Null means the caller does not request a time-zone change. Clearing a configured time zone is
     * not supported.
     */
    public void changeTimeZone(String value) {
        if (value == null) {
            return;
        }

        String normalized = value.trim();

        if (normalized.isEmpty()
                || normalized.length() > 64
                || !ZoneId.getAvailableZoneIds().contains(normalized)) {
            throw new ValidationException(InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }

        this.timeZone = normalized;
    }

    /** Pricing entry points must call this before evaluating local dates/times. */
    public ZoneId requireTimeZone() {
        if (timeZone == null || timeZone.isBlank()) {
            throw new ConflictException(InventoryErrorCode.CINEMA_TIME_ZONE_REQUIRED);
        }

        if (!ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw new ValidationException(InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }

        return ZoneId.of(timeZone);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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

    public Set<Room> getRooms() {
        return Set.copyOf(rooms);
    }

    public void addRoom(Room room) {
        if (room == null) {
            return;
        }

        rooms.add(room);
        room.assignCinema(this);
    }

    public void removeRoom(Room room) {
        if (room == null) {
            return;
        }

        rooms.remove(room);

        if (room.getCinema() == this) {
            room.assignCinema(null);
        }
    }
}
