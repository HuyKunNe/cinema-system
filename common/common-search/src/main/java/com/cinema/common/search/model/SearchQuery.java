package com.cinema.common.search.model;

import java.util.List;
import java.util.Objects;

public record SearchQuery(

        String keyword,

        List<String> fields,

        int page,

        int size,

        List<SearchSort> sorts

) {

    private static final int DEFAULT_PAGE = 0;

    private static final int DEFAULT_SIZE = 20;

    private static final int MAX_SIZE = 100;

    public SearchQuery {

        keyword = keyword == null
                ? ""
                : keyword.trim();

        fields = fields == null
                ? List.of()
                : List.copyOf(fields);

        sorts = sorts == null
                ? List.of()
                : List.copyOf(sorts);

        if (page < 0) {

            throw new IllegalArgumentException(
                    "Search page must not be negative");

        }

        if (size <= 0) {

            throw new IllegalArgumentException(
                    "Search size must be greater than zero");

        }

        if (size > MAX_SIZE) {

            throw new IllegalArgumentException(
                    "Search size must not exceed " + MAX_SIZE);

        }

        fields.forEach(
                field -> Objects.requireNonNull(
                        field,
                        "Search field must not be null"));

        sorts.forEach(
                sort -> Objects.requireNonNull(
                        sort,
                        "Search sort must not be null"));

    }

    public static SearchQuery of(
            String keyword,
            List<String> fields) {

        return new SearchQuery(
                keyword,
                fields,
                DEFAULT_PAGE,
                DEFAULT_SIZE,
                List.of());

    }

    public static SearchQuery of(
            String keyword,
            List<String> fields,
            int page,
            int size) {

        return new SearchQuery(
                keyword,
                fields,
                page,
                size,
                List.of());

    }

    public static SearchQuery matchAll() {

        return new SearchQuery(
                "",
                List.of(),
                DEFAULT_PAGE,
                DEFAULT_SIZE,
                List.of());

    }

    public SearchQuery withSort(
            SearchSort sort) {

        return new SearchQuery(
                keyword,
                fields,
                page,
                size,
                List.of(sort));

    }

    public SearchQuery withSorts(
            List<SearchSort> newSorts) {

        return new SearchQuery(
                keyword,
                fields,
                page,
                size,
                newSorts);

    }

    public boolean hasKeyword() {

        return !keyword.isBlank();

    }

    public boolean hasFields() {

        return !fields.isEmpty();

    }

}
