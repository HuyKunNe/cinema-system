package com.cinema.inventory.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.common.security.config.SecurityConfiguration;
import com.cinema.common.security.config.ServletSecurityConfiguration;
import com.cinema.inventory.config.InventorySecurityConfig;
import com.cinema.inventory.service.CinemaService;
import com.cinema.inventory.service.RoomService;
import com.cinema.inventory.service.SeatService;
import com.cinema.inventory.service.ShowtimeService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

@WebMvcTest({
        CinemaController.class,
        RoomController.class,
        SeatController.class,
        ShowtimeController.class
})
@Import({
        InventorySecurityConfig.class,
        SecurityConfiguration.class,
        ServletSecurityConfiguration.class
})
@TestPropertySource(properties = {
        "cinema.security.oauth2.issuer-uri="
                + "https://identity.cinema.test",
        "cinema.security.oauth2.jwk-set-uri="
                + "https://identity.cinema.test/oauth2/jwks",
        "cinema.security.oauth2.audience=cinema-api"
})
class InventoryEndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CinemaService cinemaService;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private SeatService seatService;

    @MockitoBean
    private ShowtimeService showtimeService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void cinemaReadsShouldBePublic()
            throws Exception {

        when(cinemaService.getActiveCinemas(null))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cinemas"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(cinemaService)
                .getActiveCinemas(null);
    }

    @Test
    void roomReadsShouldBePublic()
            throws Exception {

        UUID cinemaId = UUID.randomUUID();

        when(roomService.getActiveRooms(cinemaId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/rooms")
                        .param(
                                "cinemaId",
                                cinemaId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(roomService)
                .getActiveRooms(cinemaId);
    }

    @Test
    void seatReadsShouldBePublic()
            throws Exception {

        UUID roomId = UUID.randomUUID();

        when(seatService.getActiveSeats(roomId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/seats")
                        .param(
                                "roomId",
                                roomId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(seatService)
                .getActiveSeats(roomId);
    }

    @Test
    void showtimeReadsShouldBePublic()
            throws Exception {

        UUID movieId = UUID.randomUUID();

        when(showtimeService.getByMovieId(movieId))
                .thenReturn(List.of());

        mockMvc.perform(get(
                        "/api/v1/showtimes/by-movie/{movieId}",
                        movieId))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(showtimeService)
                .getByMovieId(movieId);
    }

    @Test
    void inventoryMutationShouldReturnUnauthorizedWithoutJwt()
            throws Exception {

        mockMvc.perform(post("/api/v1/cinemas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.timestamp")
                        .exists())
                .andExpect(jsonPath("$.error.code")
                        .value(
                                "SECURITY_AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.error.message")
                        .value("Authentication is required"))
                .andExpect(jsonPath("$.error.category")
                        .value("SECURITY"));

        verifyNoInteractions(cinemaService);
    }

    @Test
    void inventoryMutationShouldReturnForbiddenWithoutPermission()
            throws Exception {

        mockMvc.perform(post("/api/v1/cinemas")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority(
                                        "ROLE_STAFF")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.error.code")
                        .value("SECURITY_ACCESS_DENIED"))
                .andExpect(jsonPath("$.error.message")
                        .value("Access is denied"))
                .andExpect(jsonPath("$.error.category")
                        .value("SECURITY"));

        verifyNoInteractions(cinemaService);
    }

    @Test
    void inventoryMutationShouldPassSecurityWithManagePermission()
            throws Exception {

        mockMvc.perform(post("/api/v1/cinemas")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority(
                                        "inventory:manage")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cinemaService);
    }

    @Test
    void showtimeMutationShouldReturnUnauthorizedWithoutJwt()
            throws Exception {

        UUID showtimeId = UUID.randomUUID();

        mockMvc.perform(patch(
                        "/api/v1/showtimes/{showtimeId}/open",
                        showtimeId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(showtimeService);
    }

    @Test
    void showtimeMutationShouldRejectInventoryPermission()
            throws Exception {

        UUID showtimeId = UUID.randomUUID();

        mockMvc.perform(patch(
                        "/api/v1/showtimes/{showtimeId}/open",
                        showtimeId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority(
                                        "inventory:manage"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(showtimeService);
    }

    @Test
    void showtimeMutationShouldSucceedWithShowtimePermission()
            throws Exception {

        UUID showtimeId = UUID.randomUUID();

        mockMvc.perform(patch(
                        "/api/v1/showtimes/{showtimeId}/open",
                        showtimeId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority(
                                        "showtime:manage"))))
                .andExpect(status().isOk());

        verify(showtimeService)
                .openForBooking(showtimeId);
    }

    @Test
    void undeclaredEndpointShouldBeForbiddenForAuthenticatedUser()
            throws Exception {

        mockMvc.perform(get("/internal/not-exposed")
                        .with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.error.code")
                        .value("SECURITY_ACCESS_DENIED"))
                .andExpect(jsonPath("$.error.category")
                        .value("SECURITY"));

        verifyNoInteractions(
                cinemaService,
                roomService,
                seatService,
                showtimeService);
    }

    @Test
    void optionsRequestShouldBePublic()
            throws Exception {

        mockMvc.perform(options("/api/v1/showtimes"))
                .andExpect(status().is2xxSuccessful());
    }
}
