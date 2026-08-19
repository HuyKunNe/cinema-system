package com.cinema.inventory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.common.security.config.SecurityConfiguration;
import com.cinema.common.security.config.ServletSecurityConfiguration;
import com.cinema.inventory.config.InventorySecurityConfig;
import com.cinema.inventory.dto.request.GenerateShowSeatsRequest;
import com.cinema.inventory.dto.request.HoldShowSeatRequest;
import com.cinema.inventory.dto.request.ShowSeatBookingRequest;
import com.cinema.inventory.service.ShowSeatService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@WebMvcTest(ShowSeatController.class)
@Import({
    InventorySecurityConfig.class,
    SecurityConfiguration.class,
    ServletSecurityConfiguration.class
})
@TestPropertySource(
        properties = {
            "cinema.security.oauth2.issuer-uri=" + "https://identity.cinema.test",
            "cinema.security.oauth2.jwk-set-uri=" + "https://identity.cinema.test/oauth2/jwks",
            "cinema.security.oauth2.audience=cinema-api"
        })
class ShowSeatSecurityTest {

    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("100000.00");

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ShowSeatService showSeatService;

    @MockitoBean private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void getByIdShouldBePublic() throws Exception {

        mockMvc.perform(get("/api/v1/show-seats/{showSeatId}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void getByShowtimeIdShouldBePublic() throws Exception {

        mockMvc.perform(get("/api/v1/show-seats").param("showtimeId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void generateShouldReturnUnauthorizedWithoutJwt() throws Exception {

        performGenerate()
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.error.code").value("SECURITY_AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.error.message").value("Authentication is required"))
                .andExpect(jsonPath("$.error.category").value("SECURITY"));

        verifyNoInteractions(showSeatService);
    }

    @Test
    void generateShouldReturnForbiddenWithoutManagePermission() throws Exception {

        performGenerateWithUserRole()
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.error.code").value("SECURITY_ACCESS_DENIED"))
                .andExpect(jsonPath("$.error.message").value("Access is denied"))
                .andExpect(jsonPath("$.error.category").value("SECURITY"));

        verifyNoInteractions(showSeatService);
    }

    @Test
    void generateShouldSucceedWithManagePermission() throws Exception {

        UUID showtimeId = UUID.randomUUID();
        GenerateShowSeatsRequest request = new GenerateShowSeatsRequest(DEFAULT_PRICE);

        mockMvc.perform(
                        post("/api/v1/show-seats")
                                .param("showtimeId", showtimeId.toString())
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "inventory:manage")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(showSeatService).generate(showtimeId, request);
    }

    @Test
    void makeUnavailableShouldRequireManagePermission() throws Exception {

        UUID showSeatId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/show-seats/{showSeatId}/unavailable", showSeatId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/unavailable", showSeatId)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "inventory:write"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/unavailable", showSeatId)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "inventory:manage"))))
                .andExpect(status().isOk());

        verify(showSeatService).makeUnavailable(showSeatId);
    }

    @Test
    void makeAvailableShouldRequireManagePermission() throws Exception {

        UUID showSeatId = UUID.randomUUID();

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/available", showSeatId)
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/available", showSeatId)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "inventory:manage"))))
                .andExpect(status().isOk());

        verify(showSeatService).makeAvailable(showSeatId);
    }

    @Test
    void holdShouldReturnUnauthorizedWithoutJwt() throws Exception {

        performHold(UUID.randomUUID(), validHoldRequest()).andExpect(status().isUnauthorized());

        verifyNoInteractions(showSeatService);
    }

    @Test
    void holdShouldReturnForbiddenWithoutWritePermission() throws Exception {

        UUID showSeatId = UUID.randomUUID();
        HoldShowSeatRequest request = validHoldRequest();

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/hold", showSeatId)
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(showSeatService);
    }

    @Test
    void holdShouldSucceedWithWritePermission() throws Exception {

        UUID showSeatId = UUID.randomUUID();
        HoldShowSeatRequest request = validHoldRequest();

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/hold", showSeatId)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "inventory:write")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<HoldShowSeatRequest> requestCaptor =
                ArgumentCaptor.forClass(HoldShowSeatRequest.class);

        verify(showSeatService).hold(eq(showSeatId), requestCaptor.capture());

        HoldShowSeatRequest actualRequest = requestCaptor.getValue();

        assertThat(actualRequest.bookingId()).isEqualTo(request.bookingId());

        assertThat(actualRequest.expiresAt().toInstant())
                .isEqualTo(request.expiresAt().toInstant());
    }

    @Test
    void bookShouldRequireWritePermission() throws Exception {

        UUID showSeatId = UUID.randomUUID();
        ShowSeatBookingRequest request = new ShowSeatBookingRequest(UUID.randomUUID());

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/book", showSeatId)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "inventory:manage")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/book", showSeatId)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "inventory:write")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(showSeatService).book(showSeatId, request);
    }

    @Test
    void releaseShouldRequireWritePermission() throws Exception {

        UUID showSeatId = UUID.randomUUID();
        ShowSeatBookingRequest request = new ShowSeatBookingRequest(UUID.randomUUID());

        mockMvc.perform(put("/api/v1/show-seats/{showSeatId}/release", showSeatId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/release", showSeatId)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "inventory:write")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(showSeatService).release(showSeatId, request);
    }

    @Test
    void spoofedRoleHeadersShouldNotAuthorizeServiceEndpoint() throws Exception {

        UUID showSeatId = UUID.randomUUID();
        HoldShowSeatRequest request = validHoldRequest();

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/hold", showSeatId)
                                .header("X-Roles", "SERVICE")
                                .header("X-User-Id", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(showSeatService);
    }

    @Test
    void spoofedPermissionHeaderShouldNotAuthorizeAdminEndpoint() throws Exception {

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/available", UUID.randomUUID())
                                .header("X-Permissions", "inventory:manage"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(showSeatService);
    }

    private org.springframework.test.web.servlet.ResultActions performGenerate() throws Exception {

        GenerateShowSeatsRequest request = new GenerateShowSeatsRequest(DEFAULT_PRICE);

        return mockMvc.perform(
                post("/api/v1/show-seats")
                        .param("showtimeId", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));
    }

    private org.springframework.test.web.servlet.ResultActions performGenerateWithUserRole()
            throws Exception {

        GenerateShowSeatsRequest request = new GenerateShowSeatsRequest(DEFAULT_PRICE);

        return mockMvc.perform(
                post("/api/v1/show-seats")
                        .param("showtimeId", UUID.randomUUID().toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));
    }

    private org.springframework.test.web.servlet.ResultActions performHold(
            UUID showSeatId, HoldShowSeatRequest request) throws Exception {

        return mockMvc.perform(
                put("/api/v1/show-seats/{showSeatId}/hold", showSeatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));
    }

    private HoldShowSeatRequest validHoldRequest() {
        return new HoldShowSeatRequest(UUID.randomUUID(), OffsetDateTime.now().plusMinutes(10));
    }
}
