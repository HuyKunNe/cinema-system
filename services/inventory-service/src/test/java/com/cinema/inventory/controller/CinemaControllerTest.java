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

import com.cinema.inventory.dto.request.CreateCinemaRequest;
import com.cinema.inventory.dto.response.CinemaResponse;
import com.cinema.inventory.service.CinemaService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class CinemaControllerTest {

  @Mock
  private CinemaService cinemaService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();

    mockMvc = MockMvcBuilders
        .standaloneSetup(new CinemaController(cinemaService))
        .build();
  }

  @Test
  void createShouldReturnCreatedCinema() throws Exception {
    UUID cinemaId = UUID.randomUUID();

    CreateCinemaRequest request = new CreateCinemaRequest(
        "CGV Vincom",
        "72 Le Thanh Ton",
        "Ho Chi Minh");

    CinemaResponse response = new CinemaResponse(
        cinemaId,
        request.name(),
        request.address(),
        request.city(),
        true,
        OffsetDateTime.now(),
        OffsetDateTime.now());

    when(cinemaService.create(request)).thenReturn(response);

    mockMvc.perform(post("/api/v1/cinemas")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().string(
            "Location",
            "/api/v1/cinemas/" + cinemaId))
        .andExpect(jsonPath("$.id")
            .value(cinemaId.toString()))
        .andExpect(jsonPath("$.name")
            .value("CGV Vincom"))
        .andExpect(jsonPath("$.active")
            .value(true));

    verify(cinemaService).create(request);
  }

  @Test
  void createShouldReturnBadRequestWhenNameIsBlank()
      throws Exception {

    CreateCinemaRequest request = new CreateCinemaRequest(
        "",
        "72 Le Thanh Ton",
        "Ho Chi Minh");

    mockMvc.perform(post("/api/v1/cinemas")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(cinemaService);
  }

  @Test
  void getActiveCinemasShouldPassCityToService()
      throws Exception {

    CinemaResponse response = new CinemaResponse(
        UUID.randomUUID(),
        "CGV Vincom",
        "72 Le Thanh Ton",
        "Ho Chi Minh",
        true,
        OffsetDateTime.now(),
        OffsetDateTime.now());

    when(cinemaService.getActiveCinemas("Ho Chi Minh"))
        .thenReturn(List.of(response));

    mockMvc.perform(get("/api/v1/cinemas")
        .param("city", "Ho Chi Minh"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name")
            .value("CGV Vincom"));

    verify(cinemaService)
        .getActiveCinemas("Ho Chi Minh");
  }
}