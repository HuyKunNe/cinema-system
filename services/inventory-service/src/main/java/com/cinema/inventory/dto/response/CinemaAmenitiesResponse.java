package com.cinema.inventory.dto.response;

import java.util.List;
import java.util.UUID;

public record CinemaAmenitiesResponse(UUID cinemaId, Long version, List<String> amenities) {}
