package com.cinema.movie.dto.request;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.cinema.movie.entity.MovieStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMovieRequest(

        @NotBlank(message = "Movie title is required") @Size(max = 255, message = "Movie title must not exceed 255 characters") String title,

        @Size(max = 5000, message = "Description must not exceed 5000 characters") String description,

        @NotNull(message = "Duration is required") @Min(value = 1, message = "Duration must be greater than zero") Integer durationMinutes,

        LocalDate releaseDate,

        @Size(max = 500, message = "Poster URL must not exceed 500 characters") String posterUrl,

        @Size(max = 500, message = "Trailer URL must not exceed 500 characters") String trailerUrl,

        @NotNull(message = "Movie status is required") MovieStatus status,

        @NotEmpty(message = "At least one genre is required") Set<UUID> genreIds

) {
}
