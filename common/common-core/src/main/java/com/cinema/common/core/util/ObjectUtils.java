package com.cinema.common.core.util;

import java.util.Objects;

public final class ObjectUtils {

    private ObjectUtils() {
    }

    public static <T> T requireNonNull(T object, String message) {
        return Objects.requireNonNull(object, message);
    }

    public static boolean isNull(Object object) {
        return object == null;
    }

    public static boolean isNotNull(Object object) {
        return object != null;
    }
}
