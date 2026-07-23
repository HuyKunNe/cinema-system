package com.cinema.movie.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.cinema.movie.dto.request.CreateGenreRequest;
import com.cinema.movie.dto.request.UpdateGenreRequest;
import com.cinema.movie.dto.response.GenreResponse;
import com.cinema.movie.entity.Genre;

@Mapper(config = MovieMapperConfig.class)
public interface GenreMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Genre toEntity(CreateGenreRequest request);

    GenreResponse toResponse(Genre genre);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            UpdateGenreRequest request,
            @MappingTarget Genre genre);
}
