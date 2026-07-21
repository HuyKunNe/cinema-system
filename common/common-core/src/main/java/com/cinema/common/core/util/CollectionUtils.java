package com.cinema.common.core.util;

import java.util.Collection;
import java.util.List;

public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static <T> List<T> nullSafeList(List<T> list) {
        return list == null ? List.of() : list;
    }
}
