package com.cinema.inventory.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.common.security.config.SecurityConfiguration;
import com.cinema.common.security.config.ServletSecurityConfiguration;
import com.cinema.common.test.security.TestJwtIssuer;
import com.cinema.inventory.config.InventorySecurityConfig;
import com.cinema.inventory.service.ShowtimeService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
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

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@WebMvcTest(ShowtimeController.class)
@Import({
    InventorySecurityConfig.class,
    SecurityConfiguration.class,
    ServletSecurityConfiguration.class
})
class InventoryIndependentResourceServerTest {

    private static final TestJwtIssuer JWT_ISSUER =
            new TestJwtIssuer("https://identity.inventory.test", "cinema-api");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ShowtimeService showtimeService;

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
    void validJwtShouldAuthorizeShowtimeMutation() throws Exception {

        UUID showtimeId = UUID.randomUUID();

        String token =
                JWT_ISSUER.token(
                        claims ->
                                claims.claim("roles", List.of("STAFF"))
                                        .claim("permissions", List.of("showtime:manage")));

        mockMvc.perform(
                        patch("/api/v1/showtimes/{showtimeId}/open", showtimeId)
                                .header(HttpHeaders.AUTHORIZATION, JWT_ISSUER.bearer(token)))
                .andExpect(status().isOk());

        verify(showtimeService).openForBooking(showtimeId);
    }

    @Test
    void untrustedJwtShouldBeRejectedByInventoryService() throws Exception {

        UUID showtimeId = UUID.randomUUID();

        String token =
                JWT_ISSUER.untrustedToken(
                        claims -> claims.claim("permissions", List.of("showtime:manage")));

        assertUnauthorized(showtimeId, token);

        verifyNoInteractions(showtimeService);
    }

    @Test
    void jwtWithWrongIssuerShouldBeRejectedByInventoryService() throws Exception {

        UUID showtimeId = UUID.randomUUID();

        String token =
                JWT_ISSUER.token(
                        claims ->
                                claims.issuer("https://untrusted.inventory.test")
                                        .claim("permissions", List.of("showtime:manage")));

        assertUnauthorized(showtimeId, token);

        verifyNoInteractions(showtimeService);
    }

    @Test
    void expiredJwtShouldBeRejectedByInventoryService() throws Exception {

        UUID showtimeId = UUID.randomUUID();
        Instant now = Instant.now();

        String token =
                JWT_ISSUER.token(
                        claims ->
                                claims.issueTime(Date.from(now.minusSeconds(600)))
                                        .notBeforeTime(Date.from(now.minusSeconds(600)))
                                        .expirationTime(Date.from(now.minusSeconds(300)))
                                        .claim("permissions", List.of("showtime:manage")));

        assertUnauthorized(showtimeId, token);

        verifyNoInteractions(showtimeService);
    }

    @Test
    void forwardedIdentityHeadersShouldNotAuthenticateInventoryRequest() throws Exception {

        UUID showtimeId = UUID.randomUUID();

        mockMvc.perform(
                        patch("/api/v1/showtimes/{showtimeId}/open", showtimeId)
                                .header("X-User-Id", UUID.randomUUID().toString())
                                .header("X-Roles", "ADMIN")
                                .header("X-Permissions", "showtime:manage"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("SECURITY_AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(showtimeService);
    }

    private void assertUnauthorized(UUID showtimeId, String token) throws Exception {

        mockMvc.perform(
                        patch("/api/v1/showtimes/{showtimeId}/open", showtimeId)
                                .header(HttpHeaders.AUTHORIZATION, JWT_ISSUER.bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.error.category").value("SECURITY"));
    }
}
