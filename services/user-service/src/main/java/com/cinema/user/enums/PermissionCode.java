package com.cinema.user.enums;

public enum PermissionCode {

    BOOKING_CREATE("booking:create"),
    BOOKING_READ("booking:read"),
    BOOKING_CANCEL("booking:cancel"),
    MOVIE_MANAGE("movie:manage"),
    SHOWTIME_MANAGE("showtime:manage"),
    INVENTORY_MANAGE("inventory:manage"),
    PAYMENT_READ("payment:read"),
    NOTIFICATION_MANAGE("notification:manage"),
    USER_MANAGE("user:manage");

    private final String code;

    PermissionCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
