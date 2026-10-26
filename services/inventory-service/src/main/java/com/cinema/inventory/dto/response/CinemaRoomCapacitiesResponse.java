package com.cinema.inventory.dto.response;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CinemaRoomCapacitiesResponse(
        UUID cinemaId,
        long totalRoomCount,
        long activeRoomCount,
        long inactiveRoomCount,
        long unconfiguredRoomCount,
        boolean capacityComplete,
        long knownCapacity,
        Long capacity,
        List<RoomCapacity> rooms) {

    public enum CapacitySource {
        PUBLISHED_MATRIX,
        PUBLISHED_GEOMETRY,
        NOT_PUBLISHED
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record RoomCapacity(
            UUID roomId,
            String roomName,
            boolean roomActive,
            UUID layoutId,
            Long layoutVersion,
            CapacitySource source,
            Long seatUnitCount,
            Long capacity,
            Long activeSeatUnitCount,
            Long activeSeatCapacity) {}
}
