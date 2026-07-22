package com.cinema.common.search.model;

import java.util.List;

public record SearchPage<T>(

        List<SearchHit<T>> hits,

        long totalElements,

        int totalPages,

        int page,

        int size

) {

    public SearchPage {

        hits = hits == null
                ? List.of()
                : List.copyOf(hits);

        if (totalElements < 0) {

            throw new IllegalArgumentException(
                    "Total elements must not be negative");

        }

        if (totalPages < 0) {

            throw new IllegalArgumentException(
                    "Total pages must not be negative");

        }

        if (page < 0) {

            throw new IllegalArgumentException(
                    "Page must not be negative");

        }

        if (size <= 0) {

            throw new IllegalArgumentException(
                    "Size must be greater than zero");

        }

    }

    public static <T> SearchPage<T> empty(
            int page,
            int size) {

        return new SearchPage<>(
                List.of(),
                0,
                0,
                page,
                size);

    }

    public List<T> content() {

        return hits.stream()
                .map(SearchHit::content)
                .toList();

    }

    public boolean hasNext() {

        return page + 1 < totalPages;

    }

    public boolean hasPrevious() {

        return page > 0;

    }

    public boolean isEmpty() {

        return hits.isEmpty();

    }

}
