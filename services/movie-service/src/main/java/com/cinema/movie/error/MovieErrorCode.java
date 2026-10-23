package com.cinema.movie.error;

import com.cinema.common.exception.code.ErrorCategory;
import com.cinema.common.exception.code.ErrorCode;

public enum MovieErrorCode implements ErrorCode {
    MOVIE_NOT_FOUND("MOVIE_NOT_FOUND", "Movie not found", ErrorCategory.RESOURCE),

    GENRE_NOT_FOUND("GENRE_NOT_FOUND", "Genre not found", ErrorCategory.RESOURCE),

    GENRE_ALREADY_EXISTS("GENRE_ALREADY_EXISTS", "Genre already exists", ErrorCategory.BUSINESS),

    GENRE_IN_USE("GENRE_IN_USE", "Genre is being used by a movie", ErrorCategory.BUSINESS),

    INVALID_MOVIE_TITLE("INVALID_MOVIE_TITLE", "Movie title is invalid", ErrorCategory.VALIDATION),

    INVALID_TRAILER_URL(
            "INVALID_TRAILER_URL",
            "Trailer URL must be an absolute HTTP or HTTPS URL with a valid host",
            ErrorCategory.VALIDATION),

    TRAILER_URL_TOO_LONG(
            "TRAILER_URL_TOO_LONG",
            "Trailer URL must not exceed 500 characters",
            ErrorCategory.VALIDATION),

    INVALID_BACKDROP_URL(
            "INVALID_BACKDROP_URL",
            "Backdrop URL must be an absolute HTTP or HTTPS URL with a valid host",
            ErrorCategory.VALIDATION),

    BACKDROP_URL_TOO_LONG(
            "BACKDROP_URL_TOO_LONG",
            "Backdrop URL must not exceed 500 characters",
            ErrorCategory.VALIDATION),

    INVALID_HERO_QUERY(
            "INVALID_HERO_QUERY",
            "Hero limit or movie ID filter is outside the supported range",
            ErrorCategory.VALIDATION),

    INVALID_HERO_CONFIGURATION(
            "INVALID_HERO_CONFIGURATION",
            "Hero configuration requires valid fields, nonnegative priority and a valid period",
            ErrorCategory.VALIDATION),

    INVALID_CATALOG_PAGINATION(
            "INVALID_CATALOG_PAGINATION",
            "Catalog page or size is outside the supported range",
            ErrorCategory.VALIDATION),

    INVALID_CATALOG_MOVIE_IDS(
            "INVALID_CATALOG_MOVIE_IDS",
            "Catalog movie ID filter contains null IDs or exceeds the supported limit",
            ErrorCategory.VALIDATION);

    private final String code;
    private final String message;
    private final ErrorCategory category;

    MovieErrorCode(String code, String message, ErrorCategory category) {
        this.code = code;
        this.message = message;
        this.category = category;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public ErrorCategory category() {
        return category;
    }
}
