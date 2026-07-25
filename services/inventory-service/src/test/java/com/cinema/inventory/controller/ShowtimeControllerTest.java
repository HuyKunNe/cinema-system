package com.cinema.inventory.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.service.ShowtimeService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ShowtimeControllerTest {

    @Mock
    private ShowtimeService showtimeService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new ShowtimeController(showtimeService))
                .build();
    }

    @Test
    void createShouldReturnCreatedShowtime() throws Exception {
        UUID showtimeId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        OffsetDateTime startsAt = OffsetDateTime.parse("2099-01-01T03:00:00Z");

        OffsetDateTime endsAt = OffsetDateTime.parse("2099-01-01T05:00:00Z");

        CreateShowtimeRequest request = new CreateShowtimeRequest(
                movieId,
                roomId,
                startsAt,
                endsAt);

        ShowtimeResponse response = response(
                showtimeId,
                movieId,
                roomId,
                startsAt,
                endsAt,
                ShowtimeStatus.SCHEDULED);

        when(showtimeService.create(request))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/showtimes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/showtimes/" + showtimeId))
                .andExpect(jsonPath("$.movieId")
                        .value(movieId.toString()))
                .andExpect(jsonPath("$.status")
                        .value("SCHEDULED"));

        verify(showtimeService).create(request);
    }

    @Test
    void createShouldReturnBadRequestForInvalidTimeRange()
            throws Exception {

        OffsetDateTime startsAt = OffsetDateTime.parse("2099-01-01T03:00:00Z");

        OffsetDateTime endsAt = OffsetDateTime.parse("2099-01-01T05:00:00Z");

        CreateShowtimeRequest request = new CreateShowtimeRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                startsAt,
                endsAt);

        mockMvc.perform(post("/api/v1/showtimes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showtimeService);
    }

    @Test
    void getByTimeRangeShouldPassParsedDatesToService()
            throws Exception {

        OffsetDateTime from = OffsetDateTime.parse("2099-01-01T00:00:00+07:00");
        OffsetDateTime to = OffsetDateTime.parse("2099-01-02T00:00:00+07:00");

        when(showtimeService.getByTimeRange(from, to))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/showtimes")
                .param("from", from.toString())
                .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(showtimeService).getByTimeRange(from, to);
    }

    @Test
    void openShouldReturnUpdatedShowtime() throws Exception {
        UUID showtimeId = UUID.randomUUID();

        ShowtimeResponse response = response(
                showtimeId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2),
                ShowtimeStatus.OPEN_FOR_BOOKING);

        when(showtimeService.openForBooking(showtimeId))
                .thenReturn(response);

        mockMvc.perform(patch(
                "/api/v1/showtimes/{showtimeId}/open",
                showtimeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("OPEN_FOR_BOOKING"));

        verify(showtimeService).openForBooking(showtimeId);
    }

    private ShowtimeResponse response(
            UUID id,
            UUID movieId,
            UUID roomId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            ShowtimeStatus status) {

        return new ShowtimeResponse(
                id,
                movieId,
                roomId,
                "Room 01",
                UUID.randomUUID(),
                "CGV Vincom",
                startsAt,
                endsAt,
                status,
                OffsetDateTime.now(),
                OffsetDateTime.now());
    }
}