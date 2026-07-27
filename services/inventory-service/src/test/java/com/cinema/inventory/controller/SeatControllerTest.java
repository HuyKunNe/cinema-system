package com.cinema.inventory.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.cinema.inventory.dto.request.CreateSeatRequest;
import com.cinema.inventory.dto.request.UpdateSeatRequest;
import com.cinema.inventory.dto.response.SeatResponse;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.service.SeatService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SeatControllerTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-07-27T03:00:00Z");

    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-07-27T04:00:00Z");

    @Mock
    private SeatService seatService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .findAndRegisterModules();

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

        SeatResponse response = response(
                seatId,
                roomId,
                request.seatNumber(),
                request.rowLabel(),
                request.seatType(),
                true);

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
                .andExpect(jsonPath("$.id")
                        .value(seatId.toString()))
                .andExpect(jsonPath("$.roomId")
                        .value(roomId.toString()))
                .andExpect(jsonPath("$.seatNumber")
                        .value("A1"))
                .andExpect(jsonPath("$.rowLabel")
                        .value("A"))
                .andExpect(jsonPath("$.seatType")
                        .value("STANDARD"))
                .andExpect(jsonPath("$.active")
                        .value(true));

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
    void createShouldReturnBadRequestWhenRoomIdIsMissing()
            throws Exception {

        CreateSeatRequest request = new CreateSeatRequest(
                "A1",
                "A",
                SeatType.STANDARD);

        mockMvc.perform(post("/api/v1/seats")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(seatService);
    }

    @Test
    void getByIdShouldReturnSeat() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();

        SeatResponse response = response(
                seatId,
                roomId,
                "B5",
                "B",
                SeatType.VIP,
                true);

        when(seatService.getById(seatId))
                .thenReturn(response);

        mockMvc.perform(get(
                "/api/v1/seats/{seatId}",
                seatId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(seatId.toString()))
                .andExpect(jsonPath("$.roomId")
                        .value(roomId.toString()))
                .andExpect(jsonPath("$.seatNumber")
                        .value("B5"))
                .andExpect(jsonPath("$.rowLabel")
                        .value("B"))
                .andExpect(jsonPath("$.seatType")
                        .value("VIP"))
                .andExpect(jsonPath("$.active")
                        .value(true));

        verify(seatService).getById(seatId);
    }

    @Test
    void getActiveSeatsShouldReturnSeatsByRoom()
            throws Exception {

        UUID roomId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();

        SeatResponse response = response(
                seatId,
                roomId,
                "C10",
                "C",
                SeatType.COUPLE,
                true);

        when(seatService.getActiveSeats(roomId))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/seats")
                .param("roomId", roomId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(seatId.toString()))
                .andExpect(jsonPath("$[0].roomId")
                        .value(roomId.toString()))
                .andExpect(jsonPath("$[0].seatNumber")
                        .value("C10"))
                .andExpect(jsonPath("$[0].seatType")
                        .value("COUPLE"))
                .andExpect(jsonPath("$[0].active")
                        .value(true));

        verify(seatService).getActiveSeats(roomId);
    }

    @Test
    void getActiveSeatsShouldReturnBadRequestWhenRoomIdIsMissing()
            throws Exception {

        mockMvc.perform(get("/api/v1/seats"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(seatService);
    }

    @Test
    void updateShouldReturnUpdatedSeat() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();

        UpdateSeatRequest request = new UpdateSeatRequest(
                "D7",
                "D",
                SeatType.ACCESSIBLE,
                false);

        SeatResponse response = response(
                seatId,
                roomId,
                request.seatNumber(),
                request.rowLabel(),
                request.seatType(),
                request.active());

        when(seatService.update(seatId, request))
                .thenReturn(response);

        mockMvc.perform(put(
                "/api/v1/seats/{seatId}",
                seatId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(seatId.toString()))
                .andExpect(jsonPath("$.roomId")
                        .value(roomId.toString()))
                .andExpect(jsonPath("$.seatNumber")
                        .value("D7"))
                .andExpect(jsonPath("$.rowLabel")
                        .value("D"))
                .andExpect(jsonPath("$.seatType")
                        .value("ACCESSIBLE"))
                .andExpect(jsonPath("$.active")
                        .value(false));

        verify(seatService).update(seatId, request);
    }

    @Test
    void updateShouldReturnBadRequestWhenActiveIsNull()
            throws Exception {

        UUID seatId = UUID.randomUUID();

        UpdateSeatRequest request = new UpdateSeatRequest(
                "A1",
                "A",
                SeatType.STANDARD,
                null);

        mockMvc.perform(put(
                "/api/v1/seats/{seatId}",
                seatId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(seatService);
    }

    private SeatResponse response(
            UUID id,
            UUID roomId,
            String seatNumber,
            String rowLabel,
            SeatType seatType,
            boolean active) {

        return new SeatResponse(
                id,
                roomId,
                seatNumber,
                rowLabel,
                seatType,
                active,
                CREATED_AT,
                UPDATED_AT);
    }
}
