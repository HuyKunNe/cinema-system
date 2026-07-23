package com.cinema.movie.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.cinema.movie.dto.request.CreateMovieRequest;
import com.cinema.movie.dto.request.UpdateMovieRequest;
import com.cinema.movie.dto.response.MovieResponse;
import com.cinema.movie.entity.Movie;

@Mapper(config = MovieMapperConfig.class, uses = GenreMapper.class)
public interface MovieMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "genres", ignore = true)
    Movie toEntity(CreateMovieRequest request);

    MovieResponse toResponse(Movie movie);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "genres", ignore = true)
    void updateEntity(
            UpdateMovieRequest request,
            @MappingTarget Movie movie);
}
