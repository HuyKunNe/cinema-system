package com.cinema.inventory.entity;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jpa.entity.BaseEntity;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_layout_seats")
public class RoomLayoutSeat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layout_id", nullable = false, updatable = false)
    private RoomLayout layout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false, updatable = false)
    private Seat seat;

    @Column(name = "seat_number_snapshot", nullable = false, length = 20, updatable = false)
    private String seatNumberSnapshot;

    @Column(name = "row_label_snapshot", nullable = false, length = 10, updatable = false)
    private String rowLabelSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type_snapshot", nullable = false, length = 50, updatable = false)
    private SeatType seatTypeSnapshot;

    @Embedded private RoomLayoutGeometry geometry;

    protected RoomLayoutSeat() {}

    public RoomLayoutSeat(RoomLayout layout, Seat seat, RoomLayoutGeometry geometry) {

        if (layout == null || seat == null || geometry == null) {
            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
        }

        layout.requireDraft();

        if (seat.getRoom() == null || !layout.getRoom().getId().equals(seat.getRoom().getId())) {

            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_SEAT_ROOM_MISMATCH);
        }

        requireSnapshotText(seat.getSeatNumber(), 20);
        requireSnapshotText(seat.getRowLabel(), 10);

        if (seat.getSeatType() == null) {
            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
        }

        this.layout = layout;
        this.seat = seat;
        this.seatNumberSnapshot = seat.getSeatNumber();
        this.rowLabelSnapshot = seat.getRowLabel();
        this.seatTypeSnapshot = seat.getSeatType();
        this.geometry = geometry;
    }

    public RoomLayout getLayout() {
        return layout;
    }

    public Seat getSeat() {
        return seat;
    }

    public String getSeatNumberSnapshot() {
        return seatNumberSnapshot;
    }

    public String getRowLabelSnapshot() {
        return rowLabelSnapshot;
    }

    public SeatType getSeatTypeSnapshot() {
        return seatTypeSnapshot;
    }

    public RoomLayoutGeometry getGeometry() {
        return geometry;
    }

    private static void requireSnapshotText(String value, int maximumLength) {

        if (value == null || value.isBlank() || value.length() > maximumLength) {

            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
        }
    }
}
