package com.cinema.inventory.entity;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "room_layout_matrix_cells")
public class RoomLayoutMatrixCell extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layout_id", nullable = false, updatable = false)
    private RoomLayout layout;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @Column(name = "row_index", nullable = false, updatable = false)
    private int rowIndex;

    @Column(name = "column_index", nullable = false, updatable = false)
    private int columnIndex;

    @Column(name = "seat_number_in_row", nullable = false, updatable = false)
    private int number;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 50, updatable = false)
    private SeatType seatType;

    protected RoomLayoutMatrixCell() {}

    public RoomLayoutMatrixCell(
            RoomLayout layout,
            Seat seat,
            int rowIndex,
            int columnIndex,
            int number,
            SeatType seatType) {

        if (layout == null || !layout.isMatrixLayout()) {
            throw invalid();
        }

        layout.requireDraft();

        if (rowIndex < 0
                || rowIndex >= layout.getRowCount()
                || columnIndex < 0
                || columnIndex >= layout.getColumnCount()
                || number < 1
                || number > layout.getColumnCount()
                || seatType == null) {
            throw invalid();
        }

        this.layout = layout;
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
        this.number = number;
        this.seatType = seatType;

        if (seat != null) {
            bindSeat(seat);
        }
    }

    public RoomLayout getLayout() {
        return layout;
    }

    public Seat getSeat() {
        return seat;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public int getNumber() {
        return number;
    }

    public SeatType getSeatType() {
        return seatType;
    }

    public void bindSeat(Seat candidate) {
        layout.requireDraft();

        if (candidate == null
                || candidate.getRoom() == null
                || !layout.getRoom().getId().equals(candidate.getRoom().getId())) {
            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_SEAT_ROOM_MISMATCH);
        }

        if (seat != null && !seat.getId().equals(candidate.getId())) {
            throw invalid();
        }

        seat = candidate;
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
    }
}
