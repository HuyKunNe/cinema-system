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
}
