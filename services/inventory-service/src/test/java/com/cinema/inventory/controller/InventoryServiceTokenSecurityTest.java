package com.cinema.inventory.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.common.security.config.SecurityConfiguration;
import com.cinema.common.security.config.ServletSecurityConfiguration;
import com.cinema.common.test.security.TestJwtIssuer;
import com.cinema.inventory.config.InventorySecurityConfig;
import com.cinema.inventory.dto.request.ShowSeatBookingRequest;
import com.cinema.inventory.service.ShowSeatService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

@WebMvcTest(ShowSeatController.class)
@Import({
    InventorySecurityConfig.class,
    SecurityConfiguration.class,
    ServletSecurityConfiguration.class
})
class InventoryServiceTokenSecurityTest {

    private static final String SERVICE_CLIENT_ID = "booking-service";

    private static final TestJwtIssuer JWT_ISSUER =
            new TestJwtIssuer("https://identity.service-token.test", "cinema-api");

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ShowSeatService showSeatService;

    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @DynamicPropertySource
    static void configureJwtValidation(DynamicPropertyRegistry registry) {

        registry.add("cinema.security.oauth2.issuer-uri", JWT_ISSUER::issuer);

        registry.add("cinema.security.oauth2.jwk-set-uri", JWT_ISSUER::jwkSetUri);

        registry.add("cinema.security.oauth2.audience", JWT_ISSUER::audience);
    }

    @AfterAll
    static void stopJwkServer() {

        JWT_ISSUER.close();
    }

    @Test
    void serviceTokenWithInventoryWriteShouldBookShowSeat() throws Exception {

        UUID showSeatId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        ShowSeatBookingRequest request = new ShowSeatBookingRequest(bookingId);

        String token =
                JWT_ISSUER.token(
                        claims ->
                                claims.subject(SERVICE_CLIENT_ID)
                                        .claim("permissions", List.of("inventory:write")));

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/book", showSeatId)
                                .header(HttpHeaders.AUTHORIZATION, JWT_ISSUER.bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<ShowSeatBookingRequest> requestCaptor =
                ArgumentCaptor.forClass(ShowSeatBookingRequest.class);

        verify(showSeatService).book(eq(showSeatId), requestCaptor.capture());

        org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().bookingId())
                .isEqualTo(bookingId);
    }

    @Test
    void serviceTokenWithReadScopeShouldNotBookShowSeat() throws Exception {

        UUID showSeatId = UUID.randomUUID();

        ShowSeatBookingRequest request = new ShowSeatBookingRequest(UUID.randomUUID());

        String token =
                JWT_ISSUER.token(
                        claims ->
                                claims.subject(SERVICE_CLIENT_ID)
                                        .claim("permissions", List.of("inventory:read")));

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/book", showSeatId)
                                .header(HttpHeaders.AUTHORIZATION, JWT_ISSUER.bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_ACCESS_DENIED"))
                .andExpect(jsonPath("$.error.category").value("SECURITY"));

        verifyNoInteractions(showSeatService);
    }

    @Test
    void serviceTokenWithManagePermissionShouldNotReplaceWriteScope() throws Exception {

        UUID showSeatId = UUID.randomUUID();

        ShowSeatBookingRequest request = new ShowSeatBookingRequest(UUID.randomUUID());

        String token =
                JWT_ISSUER.token(
                        claims ->
                                claims.subject(SERVICE_CLIENT_ID)
                                        .claim("permissions", List.of("inventory:manage")));

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/release", showSeatId)
                                .header(HttpHeaders.AUTHORIZATION, JWT_ISSUER.bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(showSeatService);
    }

    @Test
    void serviceRoleWithoutWriteScopeShouldNotAuthorizeMutation() throws Exception {

        UUID showSeatId = UUID.randomUUID();

        ShowSeatBookingRequest request = new ShowSeatBookingRequest(UUID.randomUUID());

        String token =
                JWT_ISSUER.token(
                        claims ->
                                claims.subject(SERVICE_CLIENT_ID)
                                        .claim("roles", List.of("SERVICE"))
                                        .claim("permissions", List.of("inventory:read")));

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/book", showSeatId)
                                .header(HttpHeaders.AUTHORIZATION, JWT_ISSUER.bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(showSeatService);
    }

    @Test
    void spoofedServiceHeadersShouldNotAuthorizeMutation() throws Exception {

        UUID showSeatId = UUID.randomUUID();

        ShowSeatBookingRequest request = new ShowSeatBookingRequest(UUID.randomUUID());

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/book", showSeatId)
                                .header("X-Client-Id", SERVICE_CLIENT_ID)
                                .header("X-Roles", "SERVICE")
                                .header("X-Permissions", "inventory:write")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("SECURITY_AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(showSeatService);
    }

    @Test
    void serviceTokenWithWrongAudienceShouldBeRejected() throws Exception {

        UUID showSeatId = UUID.randomUUID();

        ShowSeatBookingRequest request = new ShowSeatBookingRequest(UUID.randomUUID());

        String token =
                JWT_ISSUER.token(
                        claims ->
                                claims.subject(SERVICE_CLIENT_ID)
                                        .audience("untrusted-api")
                                        .claim("permissions", List.of("inventory:write")));

        mockMvc.perform(
                        put("/api/v1/show-seats/{showSeatId}/book", showSeatId)
                                .header(HttpHeaders.AUTHORIZATION, JWT_ISSUER.bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("SECURITY_AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(showSeatService);
    }
}
