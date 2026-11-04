package com.cinema.inventory.enums;

public enum ProjectionTechnology {
    STANDARD,
    IMAX,
    FOUR_DX,
    SCREEN_X;

    public boolean isSupportedBy(RoomType roomType) {
        if (roomType == null) {
            return false;
        }

        return switch (this) {
            case STANDARD -> roomType == RoomType.STANDARD || roomType == RoomType.VIP;
            case IMAX -> roomType == RoomType.IMAX;
            case FOUR_DX -> roomType == RoomType.FOUR_DX;
            case SCREEN_X -> roomType == RoomType.SCREEN_X;
        };
    }
}
