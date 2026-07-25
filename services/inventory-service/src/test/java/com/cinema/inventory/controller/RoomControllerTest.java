package com.cinema.inventory.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.cinema.inventory.dto.response.RoomResponse;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

  @Mock
  private RoomService roomService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();

    mockMvc = MockMvcBuilders
        .standaloneSetup(new RoomController(roomService))
        .build();
  }

  @Test
  void createShouldReturnCreatedRoom() throws Exception {
    UUID cinemaId = UUID.randomUUID();
    UUID roomId = UUID.randomUUID();

    CreateRoomRequest request = new CreateRoomRequest("Room 01", RoomType.IMAX);

    RoomResponse response = new RoomResponse(
        roomId,
        cinemaId,
        request.name(),
        request.roomType(),
        true,
        OffsetDateTime.now(),
        OffsetDateTime.now());

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
        .andExpect(jsonPath("$.roomType")
            .value("IMAX"));

    verify(roomService).create(cinemaId, request);
  }

  @Test
  void createShouldReturnBadRequestWhenCinemaIdIsMissing()
      throws Exception {

    CreateRoomRequest request = new CreateRoomRequest("Room 01", RoomType.STANDARD);

    mockMvc.perform(post("/api/v1/rooms")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(roomService);
  }

  @Test
  void getActiveRoomsShouldReturnRooms() throws Exception {
    UUID cinemaId = UUID.randomUUID();

    RoomResponse response = new RoomResponse(
        UUID.randomUUID(),
        cinemaId,
        "Room 01",
        RoomType.STANDARD,
        true,
        OffsetDateTime.now(),
        OffsetDateTime.now());

    when(roomService.getActiveRooms(cinemaId))
        .thenReturn(List.of(response));

    mockMvc.perform(get("/api/v1/rooms")
        .param("cinemaId", cinemaId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name")
            .value("Room 01"));

    verify(roomService).getActiveRooms(cinemaId);
  }
}