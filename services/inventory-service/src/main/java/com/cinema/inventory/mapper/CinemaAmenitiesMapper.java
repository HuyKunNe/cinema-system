package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.CinemaAmenitiesResponse;
import com.cinema.inventory.entity.Cinema;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = InventoryMapperConfig.class)
public interface CinemaAmenitiesMapper {

    @Mapping(target = "cinemaId", source = "id")
    @Mapping(
            target = "amenities",
            expression = "java(cinema.getAmenities().stream().sorted().toList())")
    CinemaAmenitiesResponse toResponse(Cinema cinema);
}
