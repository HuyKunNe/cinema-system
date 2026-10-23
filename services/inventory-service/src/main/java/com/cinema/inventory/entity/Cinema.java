package com.cinema.inventory.entity;

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

    private static final String DEFAULT_TIME_ZONE = "Asia/Ho_Chi_Minh";

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "opening_hours", length = 150)
    private String openingHours;

    @Column(name = "directions", length = 1000)
    private String directions;

    @Column(name = "time_zone", length = 64)
    private String timeZone = DEFAULT_TIME_ZONE;

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

    public void updatePublicDetails(
            String phone, String email, String openingHours, String directions, String timeZone) {

        if (phone != null) {
            this.phone = normalizeOptionalText(phone, 32);
        }

        if (email != null) {
            this.email = normalizeOptionalText(email, 254);
        }

        if (openingHours != null) {
            this.openingHours = normalizeOptionalText(openingHours, 150);
        }

        if (directions != null) {
            this.directions = normalizeOptionalText(directions, 1000);
        }

        if (timeZone != null) {
            String normalized = timeZone.trim();

            if (normalized.isEmpty()
                    || normalized.length() > 64
                    || !ZoneId.getAvailableZoneIds().contains(normalized)) {

                throw new ValidationException(InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
            }

            this.timeZone = normalized;
        }
    }

    private static String normalizeOptionalText(String value, int maxLength) {

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new ValidationException(InventoryErrorCode.CINEMA_PUBLIC_DETAILS_INVALID);
        }

        return normalized.isEmpty() ? null : normalized;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public String getDirections() {
        return directions;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void changeActive(boolean active) {
        this.active = active;
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
