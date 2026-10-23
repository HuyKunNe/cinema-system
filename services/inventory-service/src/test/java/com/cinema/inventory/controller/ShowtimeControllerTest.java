package com.cinema.inventory.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.request.UpdateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.service.ShowtimeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ShowtimeControllerTest {

    private static final OffsetDateTime STARTS_AT = OffsetDateTime.parse("2099-01-01T03:00:00Z");

    private static final OffsetDateTime ENDS_AT = OffsetDateTime.parse("2099-01-01T05:00:00Z");

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-07-27T03:00:00Z");

    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-07-27T04:00:00Z");

    @Mock private ShowtimeService showtimeService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private static final BigDecimal BASE_PRICE = new BigDecimal("100000.00");
    private static final DateTimeFormatter JSON_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    @BeforeEach
    void setUp() {
        objectMapper =
                new ObjectMapper()
                        .findAndRegisterModules()
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter jacksonConverter =
                new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc =
                MockMvcBuilders.standaloneSetup(new ShowtimeController(showtimeService))
                        .setMessageConverters(jacksonConverter)
                        .build();
    }

    @Test
    void createShouldReturnCreatedShowtime() throws Exception {
        UUID showtimeId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        CreateShowtimeRequest request =
                new CreateShowtimeRequest(movieId, roomId, STARTS_AT, ENDS_AT, BASE_PRICE);

        ShowtimeResponse response =
                response(showtimeId, movieId, roomId, STARTS_AT, ENDS_AT, ShowtimeStatus.SCHEDULED);

        when(showtimeService.create(request)).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/showtimes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/showtimes/" + showtimeId))
                .andExpect(jsonPath("$.id").value(showtimeId.toString()))
                .andExpect(jsonPath("$.movieId").value(movieId.toString()))
                .andExpect(jsonPath("$.roomId").value(roomId.toString()))
                .andExpect(jsonPath("$.roomName").value("Room 01"))
                .andExpect(jsonPath("$.cinemaName").value("CGV Vincom"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        verify(showtimeService).create(request);
    }

    @Test
    void createShouldReturnBadRequestWhenEndsAtIsBeforeStartsAt() throws Exception {

        CreateShowtimeRequest request =
                new CreateShowtimeRequest(
                        UUID.randomUUID(), UUID.randomUUID(), ENDS_AT, STARTS_AT, BASE_PRICE);

        mockMvc.perform(
                        post("/api/v1/showtimes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showtimeService);
    }

    @Test
    void createShouldReturnBadRequestWhenMovieIdIsNull() throws Exception {

        CreateShowtimeRequest request =
                new CreateShowtimeRequest(null, UUID.randomUUID(), STARTS_AT, ENDS_AT, BASE_PRICE);

        mockMvc.perform(
                        post("/api/v1/showtimes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showtimeService);
    }

    @Test
    void createShouldReturnBadRequestForMalformedDate() throws Exception {

        UUID movieId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        String requestBody =
                """
                {
                  "movieId": "%s",
                  "roomId": "%s",
                  "startsAt": "invalid-date",
                  "endsAt": "2099-01-01T05:00:00Z"
                }
                """
                        .formatted(movieId, roomId);

        mockMvc.perform(
                        post("/api/v1/showtimes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showtimeService);
    }

    @Test
    void getByIdShouldReturnShowtime() throws Exception {
        UUID showtimeId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        ShowtimeResponse response =
                response(showtimeId, movieId, roomId, STARTS_AT, ENDS_AT, ShowtimeStatus.SCHEDULED);

        when(showtimeService.getById(showtimeId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/showtimes/{showtimeId}", showtimeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(showtimeId.toString()))
                .andExpect(jsonPath("$.movieId").value(movieId.toString()))
                .andExpect(jsonPath("$.roomId").value(roomId.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        verify(showtimeService).getById(showtimeId);
    }

    @Test
    void getByRoomIdShouldReturnShowtimes() throws Exception {
        UUID showtimeId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        ShowtimeResponse response =
                response(
                        showtimeId,
                        movieId,
                        roomId,
                        STARTS_AT,
                        ENDS_AT,
                        ShowtimeStatus.OPEN_FOR_BOOKING);

        when(showtimeService.getByRoomId(roomId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/showtimes/by-room/{roomId}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(showtimeId.toString()))
                .andExpect(jsonPath("$[0].roomId").value(roomId.toString()))
                .andExpect(jsonPath("$[0].status").value("OPEN_FOR_BOOKING"));

        verify(showtimeService).getByRoomId(roomId);
    }

    @Test
    void getByMovieIdShouldReturnShowtimes() throws Exception {
        UUID showtimeId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        ShowtimeResponse response =
                response(showtimeId, movieId, roomId, STARTS_AT, ENDS_AT, ShowtimeStatus.SCHEDULED);

        when(showtimeService.getByMovieId(movieId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/showtimes/by-movie/{movieId}", movieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(showtimeId.toString()))
                .andExpect(jsonPath("$[0].movieId").value(movieId.toString()));

        verify(showtimeService).getByMovieId(movieId);
    }

    @Test
    void getByTimeRangeShouldPassDatesToService() throws Exception {

        ShowtimeResponse response =
                response(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        STARTS_AT,
                        ENDS_AT,
                        ShowtimeStatus.SCHEDULED);

        when(showtimeService.getByTimeRange(STARTS_AT, ENDS_AT)).thenReturn(List.of(response));

        mockMvc.perform(
                        get("/api/v1/showtimes")
                                .param("from", STARTS_AT.toString())
                                .param("to", ENDS_AT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));

        verify(showtimeService).getByTimeRange(STARTS_AT, ENDS_AT);
    }

    @Test
    void getByTimeRangeShouldReturnBadRequestWhenFromIsMissing() throws Exception {

        mockMvc.perform(get("/api/v1/showtimes").param("to", ENDS_AT.toString()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showtimeService);
    }

    @Test
    void getByTimeRangeShouldReturnBadRequestForMalformedDate() throws Exception {

        mockMvc.perform(
                        get("/api/v1/showtimes")
                                .param("from", "invalid-date")
                                .param("to", ENDS_AT.toString()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showtimeService);
    }

    @Test
    void updateShouldReturnUpdatedShowtime() throws Exception {
        UUID showtimeId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        OffsetDateTime newStartsAt = OffsetDateTime.parse("2099-01-02T03:00:00Z");

        OffsetDateTime newEndsAt = OffsetDateTime.parse("2099-01-02T06:00:00Z");

        UpdateShowtimeRequest request =
                new UpdateShowtimeRequest(newStartsAt, newEndsAt, ShowtimeStatus.OPEN_FOR_BOOKING);

        ShowtimeResponse response =
                response(
                        showtimeId,
                        movieId,
                        roomId,
                        newStartsAt,
                        newEndsAt,
                        ShowtimeStatus.OPEN_FOR_BOOKING);

        when(showtimeService.update(showtimeId, request)).thenReturn(response);

        mockMvc.perform(
                        put("/api/v1/showtimes/{showtimeId}", showtimeId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(showtimeId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN_FOR_BOOKING"))
                .andExpect(
                        jsonPath("$.startsAt").value(newStartsAt.format(JSON_DATE_TIME_FORMATTER)))
                .andExpect(jsonPath("$.endsAt").value(newEndsAt.format(JSON_DATE_TIME_FORMATTER)));

        verify(showtimeService).update(showtimeId, request);
    }

    @Test
    void updateShouldReturnBadRequestWhenTimeRangeIsInvalid() throws Exception {

        UUID showtimeId = UUID.randomUUID();

        UpdateShowtimeRequest request =
                new UpdateShowtimeRequest(ENDS_AT, STARTS_AT, ShowtimeStatus.SCHEDULED);

        mockMvc.perform(
                        put("/api/v1/showtimes/{showtimeId}", showtimeId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showtimeService);
    }

    @Test
    void openShouldReturnUpdatedShowtime() throws Exception {
        UUID showtimeId = UUID.randomUUID();

        ShowtimeResponse response =
                response(
                        showtimeId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        STARTS_AT,
                        ENDS_AT,
                        ShowtimeStatus.OPEN_FOR_BOOKING);

        when(showtimeService.openForBooking(showtimeId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/showtimes/{showtimeId}/open", showtimeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(showtimeId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN_FOR_BOOKING"));

        verify(showtimeService).openForBooking(showtimeId);
    }

    @Test
    void closeShouldReturnClosedShowtime() throws Exception {
        UUID showtimeId = UUID.randomUUID();

        ShowtimeResponse response =
                response(
                        showtimeId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        STARTS_AT,
                        ENDS_AT,
                        ShowtimeStatus.CLOSED);

        when(showtimeService.close(showtimeId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/showtimes/{showtimeId}/close", showtimeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(showtimeId.toString()))
                .andExpect(jsonPath("$.status").value("CLOSED"));

        verify(showtimeService).close(showtimeId);
    }

    @Test
    void cancelShouldReturnCancelledShowtime() throws Exception {
        UUID showtimeId = UUID.randomUUID();

        ShowtimeResponse response =
                response(
                        showtimeId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        STARTS_AT,
                        ENDS_AT,
                        ShowtimeStatus.CANCELLED);

        when(showtimeService.cancel(showtimeId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/showtimes/{showtimeId}/cancel", showtimeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(showtimeId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(showtimeService).cancel(showtimeId);
    }

    @Test
    void completeShouldReturnCompletedShowtime() throws Exception {

        UUID showtimeId = UUID.randomUUID();

        ShowtimeResponse response =
                response(
                        showtimeId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        STARTS_AT,
                        ENDS_AT,
                        ShowtimeStatus.COMPLETED);

        when(showtimeService.complete(showtimeId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/showtimes/{showtimeId}/complete", showtimeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(showtimeId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(showtimeService).complete(showtimeId);
    }

    @Test
    void createShouldReturnBadRequestWhenBasePriceIsZero() throws Exception {

        CreateShowtimeRequest request =
                new CreateShowtimeRequest(
                        UUID.randomUUID(), UUID.randomUUID(), STARTS_AT, ENDS_AT, BigDecimal.ZERO);

        mockMvc.perform(
                        post("/api/v1/showtimes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showtimeService);
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
                CREATED_AT,
                UPDATED_AT);
    }
}
