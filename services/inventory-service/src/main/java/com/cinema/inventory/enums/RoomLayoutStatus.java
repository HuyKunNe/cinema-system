package com.cinema.inventory.enums;

public enum RoomLayoutStatus {
    DRAFT,
    PUBLISHED,
    RETIRED;

    public boolean isPublishedSnapshot() {
        return this == PUBLISHED || this == RETIRED;
    }
}
