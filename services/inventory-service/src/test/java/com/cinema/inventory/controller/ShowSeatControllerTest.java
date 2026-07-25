package com.cinema.inventory.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import com.cinema.inventory.dto.request.GenerateShowSeatsRequest;
import com.cinema.inventory.dto.response.ShowSeatResponse;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.service.ShowSeatService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ShowSeatControllerTest {

  @Mock
  private ShowSeatService showSeatService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();

    mockMvc = MockMvcBuilders
        .standaloneSetup(
            new ShowSeatController(showSeatService))
        .build();
  }

  @Test
  void generateShouldReturnCreatedShowSeats()
      throws Exception {

    UUID showtimeId = UUID.randomUUID();

    GenerateShowSeatsRequest request = new GenerateShowSeatsRequest(
        new BigDecimal("120000.00"));

    ShowSeatResponse response = showSeatResponse(showtimeId);

    when(showSeatService.generate(showtimeId, request))
        .thenReturn(List.of(response));

    mockMvc.perform(post("/api/v1/show-seats")
        .param("showtimeId", showtimeId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().string(
            "Location",
            "/api/v1/show-seats?showtimeId="
                + showtimeId))
        .andExpect(jsonPath("$[0].seatNumber")
            .value("A1"))
        .andExpect(jsonPath("$[0].status")
            .value("AVAILABLE"))
        .andExpect(jsonPath("$[0].price")
            .value(120000.00));

    verify(showSeatService).generate(showtimeId, request);
  }

  @Test
  void generateShouldReturnBadRequestForInvalidPrice()
      throws Exception {

    UUID showtimeId = UUID.randomUUID();

    GenerateShowSeatsRequest request = new GenerateShowSeatsRequest(BigDecimal.ZERO);

    mockMvc.perform(post("/api/v1/show-seats")
        .param("showtimeId", showtimeId.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(showSeatService);
  }

  @Test
  void getByShowtimeIdShouldReturnAllShowSeats()
      throws Exception {

    UUID showtimeId = UUID.randomUUID();

    when(showSeatService.getByShowtimeId(showtimeId))
        .thenReturn(List.of(showSeatResponse(showtimeId)));

    mockMvc.perform(get("/api/v1/show-seats")
        .param("showtimeId", showtimeId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].seatNumber")
            .value("A1"));

    verify(showSeatService).getByShowtimeId(showtimeId);
  }

  @Test
  void getByShowtimeIdShouldReturnOnlyAvailableSeats()
      throws Exception {

    UUID showtimeId = UUID.randomUUID();

    when(showSeatService
        .getAvailableByShowtimeId(showtimeId))
        .thenReturn(List.of(showSeatResponse(showtimeId)));

    mockMvc.perform(get("/api/v1/show-seats")
        .param("showtimeId", showtimeId.toString())
        .param("availableOnly", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status")
            .value("AVAILABLE"));

    verify(showSeatService)
        .getAvailableByShowtimeId(showtimeId);
  }

  private ShowSeatResponse showSeatResponse(
      UUID showtimeId) {

    return new ShowSeatResponse(
        UUID.randomUUID(),
        showtimeId,
        UUID.randomUUID(),
        "A1",
        SeatType.STANDARD,
        new BigDecimal("120000.00"),
        ShowSeatStatus.AVAILABLE,
        null,
        null,
        OffsetDateTime.now(),
        OffsetDateTime.now());
  }
}