package com.cinema.common.mapper.util;

import java.util.Collection;

public final class MappingUtils {

    private MappingUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

}
