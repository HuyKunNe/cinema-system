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

import com.cinema.inventory.dto.request.CreateCinemaRequest;
import com.cinema.inventory.dto.request.UpdateCinemaRequest;
import com.cinema.inventory.dto.response.CinemaResponse;
import com.cinema.inventory.service.CinemaService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class CinemaControllerTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-07-27T03:00:00Z");

    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-07-27T04:00:00Z");

    @Mock
    private CinemaService cinemaService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new CinemaController(cinemaService))
                .build();
    }

    @Test
    void createShouldReturnCreatedCinema() throws Exception {
        UUID cinemaId = UUID.randomUUID();

        CreateCinemaRequest request = new CreateCinemaRequest(
                "CGV Vincom",
                "72 Le Thanh Ton",
                "Ho Chi Minh");

        CinemaResponse response = response(
                cinemaId,
                request.name(),
                request.address(),
                request.city(),
                true);

        when(cinemaService.create(request))
                .thenReturn(response);

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
                .andExpect(jsonPath("$.address")
                        .value("72 Le Thanh Ton"))
                .andExpect(jsonPath("$.city")
                        .value("Ho Chi Minh"))
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
    void getByIdShouldReturnCinema() throws Exception {
        UUID cinemaId = UUID.randomUUID();

        CinemaResponse response = response(
                cinemaId,
                "CGV Vincom",
                "72 Le Thanh Ton",
                "Ho Chi Minh",
                true);

        when(cinemaService.getById(cinemaId))
                .thenReturn(response);

        mockMvc.perform(get(
                "/api/v1/cinemas/{cinemaId}",
                cinemaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(cinemaId.toString()))
                .andExpect(jsonPath("$.name")
                        .value("CGV Vincom"))
                .andExpect(jsonPath("$.city")
                        .value("Ho Chi Minh"))
                .andExpect(jsonPath("$.active")
                        .value(true));

        verify(cinemaService).getById(cinemaId);
    }

    @Test
    void getActiveCinemasShouldFilterByCity()
            throws Exception {

        String city = "Ho Chi Minh";
        UUID cinemaId = UUID.randomUUID();

        CinemaResponse response = response(
                cinemaId,
                "CGV Vincom",
                "72 Le Thanh Ton",
                city,
                true);

        when(cinemaService.getActiveCinemas(city))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/cinemas")
                .param("city", city))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()")
                        .value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(cinemaId.toString()))
                .andExpect(jsonPath("$[0].city")
                        .value(city))
                .andExpect(jsonPath("$[0].active")
                        .value(true));

        verify(cinemaService).getActiveCinemas(city);
    }

    @Test
    void getActiveCinemasShouldAcceptMissingCity()
            throws Exception {

        when(cinemaService.getActiveCinemas(null))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cinemas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(cinemaService).getActiveCinemas(null);
    }

    @Test
    void updateShouldReturnUpdatedCinema()
            throws Exception {

        UUID cinemaId = UUID.randomUUID();

        UpdateCinemaRequest request = new UpdateCinemaRequest(
                "CGV Vincom Center",
                "72A Le Thanh Ton",
                "Ho Chi Minh",
                false);

        CinemaResponse response = response(
                cinemaId,
                request.name(),
                request.address(),
                request.city(),
                request.active());

        when(cinemaService.update(cinemaId, request))
                .thenReturn(response);

        mockMvc.perform(put(
                "/api/v1/cinemas/{cinemaId}",
                cinemaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(cinemaId.toString()))
                .andExpect(jsonPath("$.name")
                        .value("CGV Vincom Center"))
                .andExpect(jsonPath("$.address")
                        .value("72A Le Thanh Ton"))
                .andExpect(jsonPath("$.active")
                        .value(false));

        verify(cinemaService)
                .update(cinemaId, request);
    }

    @Test
    void updateShouldReturnBadRequestWhenActiveIsNull()
            throws Exception {

        UUID cinemaId = UUID.randomUUID();

        UpdateCinemaRequest request = new UpdateCinemaRequest(
                "CGV Vincom",
                "72 Le Thanh Ton",
                "Ho Chi Minh",
                null);

        mockMvc.perform(put(
                "/api/v1/cinemas/{cinemaId}",
                cinemaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cinemaService);
    }

    private CinemaResponse response(
            UUID id,
            String name,
            String address,
            String city,
            boolean active) {

        return new CinemaResponse(
                id,
                name,
                address,
                city,
                active,
                CREATED_AT,
                UPDATED_AT);
    }
}
