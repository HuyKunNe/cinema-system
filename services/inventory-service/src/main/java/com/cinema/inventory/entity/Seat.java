package com.cinema.inventory.entity;

import java.util.HashSet;
import java.util.Set;

import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.SeatType;

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
@Table(name = "seats")
public class Seat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "seat_number", nullable = false, length = 20)
    private String seatNumber;

    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 50)
    private SeatType seatType;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "seat", fetch = FetchType.LAZY)
    private Set<ShowSeat> showSeats = new HashSet<>();

    protected Seat() {
    }

    public Seat(
            Room room,
            String seatNumber,
            String rowLabel,
            SeatType seatType) {

        this.room = room;
        this.seatNumber = seatNumber;
        this.rowLabel = rowLabel;
        this.seatType = seatType;
        this.active = true;
    }

    public Room getRoom() {
        return room;
    }

    void assignRoom(Room room) {
        this.room = room;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public SeatType getSeatType() {
        return seatType;
    }

    public void setSeatType(SeatType seatType) {
        this.seatType = seatType;
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

    public Set<ShowSeat> getShowSeats() {
        return Set.copyOf(showSeats);
    }
}
