package com.cinema.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.booking.config.BookingSecurityConfig;
import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.service.BookingCancellationService;
import com.cinema.booking.service.BookingService;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.response.model.PageInfo;
import com.cinema.common.response.model.PageResponse;
import com.cinema.common.security.config.SecurityConfiguration;
import com.cinema.common.security.config.ServletSecurityConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@WebMvcTest(BookingController.class)
@ActiveProfiles("test")
@Import({
    BookingSecurityConfig.class,
    SecurityConfiguration.class,
    ServletSecurityConfiguration.class
})
class BookingControllerSecurityTest {

    private static final UUID USER_ID = UuidGenerator.next();

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private BookingService bookingService;

    @MockitoBean private BookingCancellationService bookingCancellationService;

    @MockitoBean private JwtDecoder jwtDecoder;

    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void unauthenticatedCreateShouldBeRejected() throws Exception {

        CreateBookingRequest request =
                new CreateBookingRequest("request-1", UuidGenerator.next(), List.of("H7"));

        mockMvc.perform(
                        post("/api/v1/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingService, bookingCancellationService);
    }

    @Test
    void authenticatedCreateShouldUseJwtSubjectAsOwner() throws Exception {

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        CreateBookingRequest request =
                new CreateBookingRequest("request-1", showtimeId, List.of("H7"));

        BookingResponse response =
                response(bookingId, USER_ID, showtimeId, BookingStatus.PENDING, null);

        when(bookingService.create(eq(USER_ID), any(CreateBookingRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/bookings")
                                .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/v1/bookings/" + bookingId))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(bookingService).create(eq(USER_ID), any(CreateBookingRequest.class));
    }

    @Test
    void invalidJwtSubjectShouldBeRejectedWithoutCallingService() throws Exception {

        CreateBookingRequest request =
                new CreateBookingRequest(
                        "request-invalid-subject", UuidGenerator.next(), List.of("H7"));

        mockMvc.perform(
                        post("/api/v1/bookings")
                                .with(jwt().jwt(token -> token.subject("not-a-uuid")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_INVALID_JWT_SUBJECT"));

        verifyNoInteractions(bookingService, bookingCancellationService);
    }

    @Test
    void invalidRequestShouldNotCallService() throws Exception {

        String invalidRequest =
                """
                {
                    "clientRequestId": "",
                    "showtimeId": null,
                    "seatNumbers": []
                }
                """;

        mockMvc.perform(
                        post("/api/v1/bookings")
                                .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(bookingService, bookingCancellationService);
    }

    @Test
    void unauthenticatedFindByIdShouldBeRejected() throws Exception {

        mockMvc.perform(get("/api/v1/bookings/{bookingId}", UuidGenerator.next()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingService, bookingCancellationService);
    }

    @Test
    void authenticatedFindByIdShouldUseJwtSubjectAsOwner() throws Exception {

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        BookingResponse response =
                response(bookingId, USER_ID, showtimeId, BookingStatus.RESERVED, null);

        when(bookingService.findById(USER_ID, bookingId)).thenReturn(response);

        mockMvc.perform(
                        get("/api/v1/bookings/{bookingId}", bookingId)
                                .with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("RESERVED"));

        verify(bookingService).findById(USER_ID, bookingId);
    }

    @Test
    void unauthenticatedFindAllShouldBeRejected() throws Exception {

        mockMvc.perform(get("/api/v1/bookings")).andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingService, bookingCancellationService);
    }

    @Test
    void authenticatedFindAllShouldUseJwtSubjectAsOwner() throws Exception {

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        BookingResponse booking =
                response(bookingId, USER_ID, showtimeId, BookingStatus.PENDING, null);

        PageResponse<BookingResponse> pageResponse =
                new PageResponse<>(List.of(booking), new PageInfo(0, 20, 1, 1, true, true));

        when(bookingService.findAll(USER_ID, 0, 20)).thenReturn(pageResponse);

        mockMvc.perform(
                        get("/api/v1/bookings")
                                .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                                .param("page", "0")
                                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.content[0].userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.page.page").value(0))
                .andExpect(jsonPath("$.data.page.size").value(20))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));

        verify(bookingService).findAll(USER_ID, 0, 20);
    }

    @Test
    void invalidPaginationShouldNotCallService() throws Exception {

        mockMvc.perform(
                        get("/api/v1/bookings")
                                .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
                                .param("page", "-1")
                                .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONSTRAINT_VIOLATION"));

        verifyNoInteractions(bookingService, bookingCancellationService);
    }

    @Test
    void unauthenticatedCancellationShouldBeRejected() throws Exception {

        UUID bookingId = UuidGenerator.next();

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancel", bookingId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingService, bookingCancellationService);
    }

    @Test
    void authenticatedCancellationShouldUseJwtSubjectAsOwner() throws Exception {

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        OffsetDateTime cancelledAt = OffsetDateTime.parse("2026-08-24T10:00:00Z");

        BookingResponse response =
                response(bookingId, USER_ID, showtimeId, BookingStatus.CANCELLED, cancelledAt);

        when(bookingCancellationService.cancel(USER_ID, bookingId)).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/bookings/{bookingId}/cancel", bookingId)
                                .with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledAt").exists());

        verify(bookingCancellationService).cancel(USER_ID, bookingId);
    }

    private static BookingResponse response(
            UUID bookingId,
            UUID userId,
            UUID showtimeId,
            BookingStatus status,
            OffsetDateTime cancelledAt) {

        OffsetDateTime now = OffsetDateTime.parse("2026-08-24T10:00:00Z");

        boolean pending = status == BookingStatus.PENDING;

        return new BookingResponse(
                bookingId,
                userId,
                showtimeId,
                "request-1",
                status,
                pending ? null : new BigDecimal("250000.00"),
                pending ? null : "VND",
                now.plusMinutes(10),
                null,
                cancelledAt,
                null,
                List.of(),
                0L,
                now,
                now);
    }
}
