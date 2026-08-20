package com.cinema.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.booking.config.BookingSecurityConfig;
import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.service.BookingService;
import com.cinema.common.core.id.UuidGenerator;
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

        verifyNoInteractions(bookingService);
    }

    @Test
    void authenticatedCreateShouldUseJwtSubjectAsOwner() throws Exception {

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        CreateBookingRequest request =
                new CreateBookingRequest("request-1", showtimeId, List.of("H7"));

        BookingResponse response =
                new BookingResponse(
                        bookingId,
                        USER_ID,
                        showtimeId,
                        "request-1",
                        BookingStatus.PENDING,
                        null,
                        null,
                        OffsetDateTime.now().plusMinutes(10),
                        null,
                        null,
                        null,
                        List.of(),
                        0L,
                        OffsetDateTime.now(),
                        OffsetDateTime.now());

        org.mockito.Mockito.when(
                        bookingService.create(eq(USER_ID), any(CreateBookingRequest.class)))
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

        verifyNoInteractions(bookingService);
    }
}
