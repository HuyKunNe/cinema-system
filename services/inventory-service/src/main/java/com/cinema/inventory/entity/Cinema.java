package com.cinema.inventory.entity;

import java.util.HashSet;
import java.util.Set;

import com.cinema.common.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "cinemas")
public class Cinema extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "cinema", fetch = FetchType.LAZY)
    private Set<Room> rooms = new HashSet<>();

    protected Cinema() {
    }

    public Cinema(
            String name,
            String address,
            String city) {

        this.name = name;
        this.address = address;
        this.city = city;
        this.active = true;
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
