package com.cinema.movie.mapper;

import com.cinema.movie.dto.response.MovieScreeningVersionResponse;
import com.cinema.movie.entity.MovieScreeningVersion;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MovieMapperConfig.class)
public interface MovieScreeningVersionMapper {

    @Mapping(target = "movieId", source = "movie.id")
    MovieScreeningVersionResponse toResponse(
            MovieScreeningVersion screeningVersion);
}
