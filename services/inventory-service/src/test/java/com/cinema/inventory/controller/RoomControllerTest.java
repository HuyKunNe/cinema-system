package com.cinema.inventory.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.cinema.inventory.dto.request.CreateRoomRequest;
import com.cinema.inventory.dto.request.UpdateRoomRequest;
import com.cinema.inventory.dto.response.RoomResponse;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-07-27T03:00:00Z");

    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-07-27T04:00:00Z");

    @Mock
    private RoomService roomService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new RoomController(roomService))
                .build();
    }

    @Test
    void createShouldReturnCreatedRoom() throws Exception {
        UUID cinemaId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        CreateRoomRequest request = new CreateRoomRequest(
                "Room 01",
                RoomType.STANDARD);

        RoomResponse response = response(
                roomId,
                cinemaId,
                request.name(),
                request.roomType(),
                true);

        when(roomService.create(cinemaId, request))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/rooms")
                .param("cinemaId", cinemaId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/rooms/" + roomId))
                .andExpect(jsonPath("$.id")
                        .value(roomId.toString()))
                .andExpect(jsonPath("$.cinemaId")
                        .value(cinemaId.toString()))
                .andExpect(jsonPath("$.name")
                        .value("Room 01"))
                .andExpect(jsonPath("$.roomType")
                        .value("STANDARD"))
                .andExpect(jsonPath("$.active")
                        .value(true));

        verify(roomService).create(cinemaId, request);
    }

    @Test
    void createShouldReturnBadRequestWhenNameIsBlank()
            throws Exception {

        UUID cinemaId = UUID.randomUUID();

        CreateRoomRequest request = new CreateRoomRequest(
                "",
                RoomType.STANDARD);

        mockMvc.perform(post("/api/v1/rooms")
                .param("cinemaId", cinemaId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(roomService);
    }

    @Test
    void createShouldReturnBadRequestWhenCinemaIdIsMissing()
            throws Exception {

        CreateRoomRequest request = new CreateRoomRequest(
                "Room 01",
                RoomType.STANDARD);

        mockMvc.perform(post("/api/v1/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(roomService);
    }

    @Test
    void getByIdShouldReturnRoom() throws Exception {
        UUID cinemaId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        RoomResponse response = response(
                roomId,
                cinemaId,
                "IMAX Room",
                RoomType.IMAX,
                true);

        when(roomService.getById(roomId))
                .thenReturn(response);

        mockMvc.perform(get(
                "/api/v1/rooms/{roomId}",
                roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(roomId.toString()))
                .andExpect(jsonPath("$.cinemaId")
                        .value(cinemaId.toString()))
                .andExpect(jsonPath("$.name")
                        .value("IMAX Room"))
                .andExpect(jsonPath("$.roomType")
                        .value("IMAX"))
                .andExpect(jsonPath("$.active")
                        .value(true));

        verify(roomService).getById(roomId);
    }

    @Test
    void getActiveRoomsShouldReturnRoomsByCinema()
            throws Exception {

        UUID cinemaId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        RoomResponse response = response(
                roomId,
                cinemaId,
                "Room 01",
                RoomType.STANDARD,
                true);

        when(roomService.getActiveRooms(cinemaId))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/rooms")
                .param("cinemaId", cinemaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()")
                        .value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(roomId.toString()))
                .andExpect(jsonPath("$[0].cinemaId")
                        .value(cinemaId.toString()))
                .andExpect(jsonPath("$[0].roomType")
                        .value("STANDARD"))
                .andExpect(jsonPath("$[0].active")
                        .value(true));

        verify(roomService).getActiveRooms(cinemaId);
    }

    @Test
    void getActiveRoomsShouldReturnBadRequestWhenCinemaIdIsMissing()
            throws Exception {

        mockMvc.perform(get("/api/v1/rooms"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(roomService);
    }

    @Test
    void updateShouldReturnUpdatedRoom() throws Exception {
        UUID cinemaId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        UpdateRoomRequest request = new UpdateRoomRequest(
                "VIP Room",
                RoomType.VIP,
                false);

        RoomResponse response = response(
                roomId,
                cinemaId,
                request.name(),
                request.roomType(),
                request.active());

        when(roomService.update(roomId, request))
                .thenReturn(response);

        mockMvc.perform(put(
                "/api/v1/rooms/{roomId}",
                roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(roomId.toString()))
                .andExpect(jsonPath("$.cinemaId")
                        .value(cinemaId.toString()))
                .andExpect(jsonPath("$.name")
                        .value("VIP Room"))
                .andExpect(jsonPath("$.roomType")
                        .value("VIP"))
                .andExpect(jsonPath("$.active")
                        .value(false));

        verify(roomService).update(roomId, request);
    }

    @Test
    void updateShouldReturnBadRequestWhenActiveIsNull()
            throws Exception {

        UUID roomId = UUID.randomUUID();

        UpdateRoomRequest request = new UpdateRoomRequest(
                "Room 01",
                RoomType.STANDARD,
                null);

        mockMvc.perform(put(
                "/api/v1/rooms/{roomId}",
                roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(roomService);
    }

    private RoomResponse response(
            UUID id,
            UUID cinemaId,
            String name,
            RoomType roomType,
            boolean active) {

        return new RoomResponse(
                id,
                cinemaId,
                name,
                roomType,
                active,
                CREATED_AT,
                UPDATED_AT);
    }
}
