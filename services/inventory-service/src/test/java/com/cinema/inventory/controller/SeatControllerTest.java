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

import com.cinema.inventory.dto.request.CreateSeatRequest;
import com.cinema.inventory.dto.response.SeatResponse;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.service.SeatService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SeatControllerTest {

  @Mock
  private SeatService seatService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();

    mockMvc = MockMvcBuilders
        .standaloneSetup(new SeatController(seatService))
        .build();
  }

  @Test
  void createShouldReturnCreatedSeat() throws Exception {
    UUID roomId = UUID.randomUUID();
    UUID seatId = UUID.randomUUID();

    CreateSeatRequest request = new CreateSeatRequest(
        "A1",
        "A",
        SeatType.STANDARD);

    SeatResponse response = new SeatResponse(
        seatId,
        roomId,
        request.seatNumber(),
        request.rowLabel(),
        request.seatType(),
        true,
        OffsetDateTime.now(),
        OffsetDateTime.now());

    when(seatService.create(roomId, request))
        .thenReturn(response);

    mockMvc.perform(post("/api/v1/seats")
        .param("roomId", roomId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().string(
            "Location",
            "/api/v1/seats/" + seatId))
        .andExpect(jsonPath("$.seatNumber")
            .value("A1"))
        .andExpect(jsonPath("$.seatType")
            .value("STANDARD"));

    verify(seatService).create(roomId, request);
  }

  @Test
  void createShouldReturnBadRequestWhenSeatNumberIsBlank()
      throws Exception {

    UUID roomId = UUID.randomUUID();

    CreateSeatRequest request = new CreateSeatRequest(
        "",
        "A",
        SeatType.STANDARD);

    mockMvc.perform(post("/api/v1/seats")
        .param("roomId", roomId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(seatService);
  }

  @Test
  void getActiveSeatsShouldReturnSeats() throws Exception {
    UUID roomId = UUID.randomUUID();

    SeatResponse response = new SeatResponse(
        UUID.randomUUID(),
        roomId,
        "A1",
        "A",
        SeatType.STANDARD,
        true,
        OffsetDateTime.now(),
        OffsetDateTime.now());

    when(seatService.getActiveSeats(roomId))
        .thenReturn(List.of(response));

    mockMvc.perform(get("/api/v1/seats")
        .param("roomId", roomId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].seatNumber")
            .value("A1"));

    verify(seatService).getActiveSeats(roomId);
  }
}