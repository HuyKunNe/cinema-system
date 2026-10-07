package com.cinema.movie.service;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.movie.error.MovieErrorCode;

import java.util.List;
import java.util.UUID;

public final class MovieCatalogMovieIdFilter {

    private MovieCatalogMovieIdFilter() {}

    public static List<UUID> normalize(List<UUID> movieIds, int maxMovieIds) {

        if (movieIds == null) {
            return null;
        }

        if (movieIds.size() > maxMovieIds
                || movieIds.stream().anyMatch(id -> id == null)) {

            throw new ValidationException(MovieErrorCode.INVALID_CATALOG_MOVIE_IDS);
        }

        return movieIds.stream().distinct().toList();
    }
}
