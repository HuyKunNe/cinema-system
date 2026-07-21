package com.cinema.common.core.id;

import java.util.UUID;

public final class UuidUtils {

    private UuidUtils() {
    }

    public static boolean isNull(UUID uuid) {
        return uuid == null;
    }

    public static boolean isNotNull(UUID uuid) {
        return uuid != null;
    }

}
