package com.cinema.common.core.util;

public final class EnumUtils {

    private EnumUtils() {
    }

    public static <E extends Enum<E>> boolean contains(Class<E> enumClass, String value) {
        if (value == null) {
            return false;
        }

        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return true;
            }
        }

        return false;
    }

    public static <E extends Enum<E>> E fromName(Class<E> enumClass, String value) {
        if (value == null) {
            return null;
        }

        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }

        return null;
    }
}
