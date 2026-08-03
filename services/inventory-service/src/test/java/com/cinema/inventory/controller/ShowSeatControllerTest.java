package com.cinema.inventory.controller;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.cinema.inventory.dto.request.GenerateShowSeatsRequest;
import com.cinema.inventory.dto.request.HoldShowSeatRequest;
import com.cinema.inventory.dto.request.ShowSeatBookingRequest;
import com.cinema.inventory.dto.response.ShowSeatResponse;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.service.ShowSeatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@ExtendWith(MockitoExtension.class)
class ShowSeatControllerTest {

    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("120000.00");

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse(
            "2026-07-27T03:00:00Z");

    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse(
            "2026-07-27T04:00:00Z");

    private static final OffsetDateTime HOLD_EXPIRES_AT = OffsetDateTime.parse(
            "2099-01-01T03:10:00Z");

    @Mock
    private ShowSeatService showSeatService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(
                objectMapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new ShowSeatController(
                                showSeatService))
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void generateShouldReturnCreatedShowSeats()
            throws Exception {

        UUID showtimeId = UUID.randomUUID();
        UUID showSeatId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();

        GenerateShowSeatsRequest request = new GenerateShowSeatsRequest(
                DEFAULT_PRICE);

        ShowSeatResponse response = response(
                showSeatId,
                showtimeId,
                seatId,
                "A1",
                SeatType.STANDARD,
                DEFAULT_PRICE,
                ShowSeatStatus.AVAILABLE,
                null,
                null);

        when(showSeatService.generate(
                showtimeId,
                request))
                .thenReturn(List.of(response));

        mockMvc.perform(post("/api/v1/show-seats")
                .param(
                        "showtimeId",
                        showtimeId.toString())
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper
                                .writeValueAsString(
                                        request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/show-seats?showtimeId="
                                + showtimeId))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()")
                        .value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(showSeatId.toString()))
                .andExpect(jsonPath("$[0].showtimeId")
                        .value(showtimeId.toString()))
                .andExpect(jsonPath("$[0].seatId")
                        .value(seatId.toString()))
                .andExpect(jsonPath("$[0].seatNumber")
                        .value("A1"))
                .andExpect(jsonPath("$[0].seatType")
                        .value("STANDARD"))
                .andExpect(jsonPath("$[0].price")
                        .value(120000.00))
                .andExpect(jsonPath("$[0].status")
                        .value("AVAILABLE"))
                .andExpect(jsonPath(
                        "$[0].heldByBookingId")
                        .doesNotExist())
                .andExpect(jsonPath(
                        "$[0].holdExpiresAt")
                        .doesNotExist())
                .andExpect(jsonPath("$[0].createdAt")
                        .value(
                                "2026-07-27T03:00:00Z"))
                .andExpect(jsonPath("$[0].updatedAt")
                        .value(
                                "2026-07-27T04:00:00Z"));

        verify(showSeatService)
                .generate(showtimeId, request);
    }

    @Test
    void generateShouldReturnBadRequestWhenPriceIsZero()
            throws Exception {

        GenerateShowSeatsRequest request = new GenerateShowSeatsRequest(
                BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/show-seats")
                .param(
                        "showtimeId",
                        UUID.randomUUID().toString())
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper
                                .writeValueAsString(
                                        request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showSeatService);
    }

    @Test
    void generateShouldReturnBadRequestWhenPriceIsNull()
            throws Exception {

        GenerateShowSeatsRequest request = new GenerateShowSeatsRequest(null);

        mockMvc.perform(post("/api/v1/show-seats")
                .param(
                        "showtimeId",
                        UUID.randomUUID().toString())
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper
                                .writeValueAsString(
                                        request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showSeatService);
    }

    @Test
    void generateShouldReturnBadRequestWhenShowtimeIdIsMissing()
            throws Exception {

        GenerateShowSeatsRequest request = new GenerateShowSeatsRequest(
                DEFAULT_PRICE);

        mockMvc.perform(post("/api/v1/show-seats")
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper
                                .writeValueAsString(
                                        request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showSeatService);
    }

    @Test
    void generateShouldReturnBadRequestForMalformedShowtimeId()
            throws Exception {

        GenerateShowSeatsRequest request = new GenerateShowSeatsRequest(
                DEFAULT_PRICE);

        mockMvc.perform(post("/api/v1/show-seats")
                .param(
                        "showtimeId",
                        "invalid-uuid")
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper
                                .writeValueAsString(
                                        request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showSeatService);
    }

    @Test
    void getByIdShouldReturnShowSeat()
            throws Exception {

        UUID showSeatId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();

        ShowSeatResponse response = response(
                showSeatId,
                showtimeId,
                seatId,
                "B5",
                SeatType.VIP,
                new BigDecimal("150000.00"),
                ShowSeatStatus.HELD,
                UUID.randomUUID(),
                HOLD_EXPIRES_AT);

        when(showSeatService.getById(showSeatId))
                .thenReturn(response);

        mockMvc.perform(get(
                "/api/v1/show-seats/{showSeatId}",
                showSeatId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(showSeatId.toString()))
                .andExpect(jsonPath("$.showtimeId")
                        .value(showtimeId.toString()))
                .andExpect(jsonPath("$.seatId")
                        .value(seatId.toString()))
                .andExpect(jsonPath("$.seatNumber")
                        .value("B5"))
                .andExpect(jsonPath("$.seatType")
                        .value("VIP"))
                .andExpect(jsonPath("$.price")
                        .value(150000.00))
                .andExpect(jsonPath("$.status")
                        .value("HELD"));

        verify(showSeatService)
                .getById(showSeatId);
    }

    @Test
    void getByShowtimeIdShouldReturnAllShowSeats()
            throws Exception {

        UUID showtimeId = UUID.randomUUID();

        ShowSeatResponse availableSeat = response(
                UUID.randomUUID(),
                showtimeId,
                UUID.randomUUID(),
                "A1",
                SeatType.STANDARD,
                DEFAULT_PRICE,
                ShowSeatStatus.AVAILABLE,
                null,
                null);

        ShowSeatResponse bookedSeat = response(
                UUID.randomUUID(),
                showtimeId,
                UUID.randomUUID(),
                "A2",
                SeatType.STANDARD,
                DEFAULT_PRICE,
                ShowSeatStatus.BOOKED,
                null,
                null);

        when(showSeatService
                .getByShowtimeId(showtimeId))
                .thenReturn(List.of(
                        availableSeat,
                        bookedSeat));

        mockMvc.perform(get("/api/v1/show-seats")
                .param(
                        "showtimeId",
                        showtimeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()")
                        .value(2))
                .andExpect(jsonPath("$[0].seatNumber")
                        .value("A1"))
                .andExpect(jsonPath("$[0].status")
                        .value("AVAILABLE"))
                .andExpect(jsonPath("$[1].seatNumber")
                        .value("A2"))
                .andExpect(jsonPath("$[1].status")
                        .value("BOOKED"));

        verify(showSeatService)
                .getByShowtimeId(showtimeId);
    }

    @Test
    void getByShowtimeIdShouldReturnOnlyAvailableSeats()
            throws Exception {

        UUID showtimeId = UUID.randomUUID();

        ShowSeatResponse availableSeat = response(
                UUID.randomUUID(),
                showtimeId,
                UUID.randomUUID(),
                "C10",
                SeatType.COUPLE,
                new BigDecimal("200000.00"),
                ShowSeatStatus.AVAILABLE,
                null,
                null);

        when(showSeatService
                .getAvailableByShowtimeId(showtimeId))
                .thenReturn(List.of(availableSeat));

        mockMvc.perform(get("/api/v1/show-seats")
                .param(
                        "showtimeId",
                        showtimeId.toString())
                .param(
                        "availableOnly",
                        "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()")
                        .value(1))
                .andExpect(jsonPath("$[0].seatNumber")
                        .value("C10"))
                .andExpect(jsonPath("$[0].seatType")
                        .value("COUPLE"))
                .andExpect(jsonPath("$[0].status")
                        .value("AVAILABLE"));

        verify(showSeatService)
                .getAvailableByShowtimeId(showtimeId);
    }

    @Test
    void getByShowtimeIdShouldReturnBadRequestWhenIdIsMissing()
            throws Exception {

        mockMvc.perform(get("/api/v1/show-seats"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showSeatService);
    }

    @Test
    void holdShouldReturnHeldShowSeat()
            throws Exception {

        UUID showSeatId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        HoldShowSeatRequest request = new HoldShowSeatRequest(
                bookingId,
                HOLD_EXPIRES_AT);

        ShowSeatResponse response = response(
                showSeatId,
                showtimeId,
                seatId,
                "A1",
                SeatType.STANDARD,
                DEFAULT_PRICE,
                ShowSeatStatus.HELD,
                bookingId,
                HOLD_EXPIRES_AT);

        when(showSeatService.hold(
                showSeatId,
                request))
                .thenReturn(response);

        mockMvc.perform(put(
                "/api/v1/show-seats/{showSeatId}/hold",
                showSeatId)
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper
                                .writeValueAsString(
                                        request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(showSeatId.toString()))
                .andExpect(jsonPath("$.status")
                        .value("HELD"))
                .andExpect(jsonPath("$.heldByBookingId")
                        .value(bookingId.toString()))
                .andExpect(jsonPath("$.holdExpiresAt")
                        .value(
                                "2099-01-01T03:10:00Z"));

        verify(showSeatService)
                .hold(showSeatId, request);
    }

    @Test
    void bookShouldReturnBookedShowSeat()
            throws Exception {

        UUID showSeatId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        ShowSeatBookingRequest request = new ShowSeatBookingRequest(bookingId);

        ShowSeatResponse response = response(
                showSeatId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "A1",
                SeatType.STANDARD,
                DEFAULT_PRICE,
                ShowSeatStatus.BOOKED,
                null,
                null);

        when(showSeatService.book(
                showSeatId,
                request))
                .thenReturn(response);

        mockMvc.perform(put(
                "/api/v1/show-seats/{showSeatId}/book",
                showSeatId)
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper
                                .writeValueAsString(
                                        request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(showSeatId.toString()))
                .andExpect(jsonPath("$.status")
                        .value("BOOKED"))
                .andExpect(jsonPath("$.heldByBookingId")
                        .doesNotExist())
                .andExpect(jsonPath("$.holdExpiresAt")
                        .doesNotExist());

        verify(showSeatService)
                .book(showSeatId, request);
    }

    @Test
    void releaseShouldReturnAvailableShowSeat()
            throws Exception {

        UUID showSeatId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        ShowSeatBookingRequest request = new ShowSeatBookingRequest(bookingId);

        ShowSeatResponse response = response(
                showSeatId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "A1",
                SeatType.STANDARD,
                DEFAULT_PRICE,
                ShowSeatStatus.AVAILABLE,
                null,
                null);

        when(showSeatService.release(
                showSeatId,
                request))
                .thenReturn(response);

        mockMvc.perform(put(
                "/api/v1/show-seats/{showSeatId}/release",
                showSeatId)
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper
                                .writeValueAsString(
                                        request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(showSeatId.toString()))
                .andExpect(jsonPath("$.status")
                        .value("AVAILABLE"))
                .andExpect(jsonPath("$.heldByBookingId")
                        .doesNotExist())
                .andExpect(jsonPath("$.holdExpiresAt")
                        .doesNotExist());

        verify(showSeatService)
                .release(showSeatId, request);
    }

    @Test
    void holdShouldReturnBadRequestWhenBookingIdIsNull()
            throws Exception {

        HoldShowSeatRequest request = new HoldShowSeatRequest(
                null,
                HOLD_EXPIRES_AT);

        performHold(UUID.randomUUID(), request)
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showSeatService);
    }

    @Test
    void holdShouldReturnBadRequestWhenExpiresAtIsNull()
            throws Exception {

        HoldShowSeatRequest request = new HoldShowSeatRequest(
                UUID.randomUUID(),
                null);

        performHold(UUID.randomUUID(), request)
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showSeatService);
    }

    @Test
    void holdShouldReturnBadRequestWhenExpiresAtIsPast()
            throws Exception {

        HoldShowSeatRequest request = new HoldShowSeatRequest(
                UUID.randomUUID(),
                OffsetDateTime.parse(
                        "2000-01-01T00:00:00Z"));

        performHold(UUID.randomUUID(), request)
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showSeatService);
    }

    @Test
    void bookShouldReturnBadRequestWhenBookingIdIsNull()
            throws Exception {

        ShowSeatBookingRequest request = new ShowSeatBookingRequest(null);

        mockMvc.perform(put(
                "/api/v1/show-seats/{showSeatId}/book",
                UUID.randomUUID())
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper
                                .writeValueAsString(
                                        request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showSeatService);
    }

    @Test
    void releaseShouldReturnBadRequestWhenBookingIdIsNull()
            throws Exception {

        ShowSeatBookingRequest request = new ShowSeatBookingRequest(null);

        mockMvc.perform(put(
                "/api/v1/show-seats/{showSeatId}/release",
                UUID.randomUUID())
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper
                                .writeValueAsString(
                                        request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(showSeatService);
    }

    private org.springframework.test.web.servlet.ResultActions performHold(
            UUID showSeatId,
            HoldShowSeatRequest request)
            throws Exception {

        return mockMvc.perform(put(
                "/api/v1/show-seats/{showSeatId}/hold",
                showSeatId)
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper
                                .writeValueAsString(
                                        request)));
    }

    private ShowSeatResponse response(
            UUID id,
            UUID showtimeId,
            UUID seatId,
            String seatNumber,
            SeatType seatType,
            BigDecimal price,
            ShowSeatStatus status,
            UUID heldByBookingId,
            OffsetDateTime holdExpiresAt) {

        return new ShowSeatResponse(
                id,
                showtimeId,
                seatId,
                seatNumber,
                seatType,
                price,
                status,
                heldByBookingId,
                holdExpiresAt,
                CREATED_AT,
                UPDATED_AT);
    }
}
