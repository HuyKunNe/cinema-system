package com.cinema.common.validation;

import java.util.UUID;

public final class UuidVersions {

    private UuidVersions() {}

    public static boolean isV5OrV7(UUID value) {
        return value != null && (value.version() == 5 || value.version() == 7);
    }
}
