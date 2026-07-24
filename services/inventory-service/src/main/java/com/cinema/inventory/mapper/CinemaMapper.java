package com.cinema.inventory.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.cinema.inventory.dto.response.CinemaResponse;
import com.cinema.inventory.entity.Cinema;

@Mapper(config = InventoryMapperConfig.class)
public interface CinemaMapper {

    CinemaResponse toResponse(Cinema cinema);

    List<CinemaResponse> toResponses(List<Cinema> cinemas);
}