package com.cinema.inventory.mapper;

import com.cinema.inventory.dto.response.RoomContentFormatsResponse;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.enums.ContentFormat;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;

@Mapper(
        config = InventoryMapperConfig.class,
        imports = {Arrays.class, ContentFormat.class})
public interface RoomContentFormatsMapper {

    @Mapping(target = "roomId", source = "id")
    @Mapping(
            target = "formats",
            expression =
                    "java(room.getSupportedContentFormats().stream().sorted().toList())")
    @Mapping(
            target = "supportedFormatOptions",
            expression =
                    "java(Arrays.stream(ContentFormat.values()).toList())")
    RoomContentFormatsResponse toResponse(Room room);
}
