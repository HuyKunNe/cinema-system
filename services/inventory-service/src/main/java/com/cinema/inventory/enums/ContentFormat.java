package com.cinema.inventory.enums;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ContentFormat {
    TWO_D("2D"),
    THREE_D("3D");

    private final String code;

    ContentFormat(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ContentFormat fromCode(String code) {
        for (ContentFormat format : values()) {
            if (format.code.equals(code)) {
                return format;
            }
        }

        throw new ValidationException(InventoryErrorCode.ROOM_CONTENT_FORMATS_INVALID);
    }
}
