package com.cinema.common.mapper.mapper;

import java.util.List;
import java.util.Set;

public final class CollectionMapper {

    private CollectionMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <T> List<T> toList(List<T> source) {
        return List.copyOf(source);
    }

    public static <T> Set<T> toSet(Set<T> source) {
        return Set.copyOf(source);
    }

}