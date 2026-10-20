package com.cinema.inventory.entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;

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

    @Column(
            name = "layout_managed",
            nullable = false,
            updatable = false)
    private boolean layoutManaged = false;

    @OneToMany(mappedBy = "seat", fetch = FetchType.LAZY)
    private Set<ShowSeat> showSeats = new HashSet<>();

    protected Seat() {}

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

    public static Seat createForLayout(
            Room room,
            String seatNumber,
            String rowLabel,
            SeatType seatType) {

        if (room == null
                || room.getId() == null
                || seatNumber == null
                || seatNumber.isBlank()
                || seatNumber.length() > 20
                || rowLabel == null
                || rowLabel.isBlank()
                || rowLabel.length() > 10
                || seatType == null) {
            throw new ValidationException(
                    InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
        }

        Seat seat = new Seat(
                room,
                seatNumber,
                rowLabel,
                seatType);

        seat.layoutManaged = true;

        return seat;
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
        requireMutableMetadata(
                Objects.equals(this.seatNumber, seatNumber));

        this.seatNumber = seatNumber;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        requireMutableMetadata(
                Objects.equals(this.rowLabel, rowLabel));

        this.rowLabel = rowLabel;
    }

    public SeatType getSeatType() {
        return seatType;
    }

    public void setSeatType(SeatType seatType) {
        requireMutableMetadata(this.seatType == seatType);

        this.seatType = seatType;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isLayoutManaged() {
        return layoutManaged;
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

    private void requireMutableMetadata(boolean unchanged) {
        if (layoutManaged && !unchanged) {
            throw new ConflictException(
                    InventoryErrorCode.SEAT_LAYOUT_METADATA_READ_ONLY);
        }
    }
}
