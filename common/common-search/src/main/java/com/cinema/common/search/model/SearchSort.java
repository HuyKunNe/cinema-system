package com.cinema.common.search.model;

import java.util.Objects;

public record SearchSort(

        String field,

        SearchDirection direction

) {

    public SearchSort {

        if (field == null || field.isBlank()) {

            throw new IllegalArgumentException(
                    "Search sort field must not be blank");

        }

        direction = Objects.requireNonNullElse(
                direction,
                SearchDirection.ASC);

    }

    public static SearchSort asc(
            String field) {

        return new SearchSort(
                field,
                SearchDirection.ASC);

    }

    public static SearchSort desc(
            String field) {

        return new SearchSort(
                field,
                SearchDirection.DESC);

    }

}
