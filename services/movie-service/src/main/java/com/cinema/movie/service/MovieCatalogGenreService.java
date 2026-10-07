package com.cinema.movie.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.movie.config.MovieCatalogProperties;
import com.cinema.movie.dto.response.GenreResponse;
import com.cinema.movie.entity.Genre;
import com.cinema.movie.entity.MovieStatus;
import com.cinema.movie.mapper.GenreMapper;
import com.cinema.movie.repository.MovieRepository;

@Service
@Transactional(readOnly = true)
public class MovieCatalogGenreService {

    private final MovieRepository movieRepository;
    private final GenreMapper genreMapper;
    private final MovieCatalogProperties properties;

    public MovieCatalogGenreService(
            MovieRepository movieRepository,
            GenreMapper genreMapper,
            MovieCatalogProperties properties) {

        this.movieRepository = movieRepository;
        this.genreMapper = genreMapper;
        this.properties = properties;
    }

    public List<GenreResponse> findGenres(MovieStatus status, List<UUID> movieIds) {

        List<UUID> normalizedMovieIds =
                MovieCatalogMovieIdFilter.normalize(movieIds, properties.getMaxMovieIds());

        // An explicit empty candidate set must remain empty.
        if (normalizedMovieIds != null && normalizedMovieIds.isEmpty()) {
            return List.of();
        }

        List<Genre> genres =
                normalizedMovieIds == null
                        ? movieRepository.findCatalogGenres(status)
                        : movieRepository.findCatalogGenresByMovieIds(status, normalizedMovieIds);

        return genres.stream().map(genreMapper::toResponse).toList();
    }
}
