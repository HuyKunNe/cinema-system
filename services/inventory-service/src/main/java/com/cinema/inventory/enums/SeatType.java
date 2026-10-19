package com.cinema.inventory.enums;

public enum SeatType {
    STANDARD(1),
    VIP(1),
    COUPLE(2),
    ACCESSIBLE(1);

    private final int capacity;

    SeatType(int capacity) {
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * Horizontal space occupied in a room matrix. This is separate from the number of people the
     * seat accommodates.
     */
    public int getMatrixColumnSpan() {
        return this == COUPLE ? 2 : 1;
    }
}
