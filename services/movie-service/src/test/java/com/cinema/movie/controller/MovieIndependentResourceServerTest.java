package com.cinema.movie.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.common.security.config.SecurityConfiguration;
import com.cinema.common.security.config.ServletSecurityConfiguration;
import com.cinema.common.test.security.TestJwtIssuer;
import com.cinema.movie.config.MovieSecurityConfig;
import com.cinema.movie.service.GenreService;
import com.cinema.movie.service.MovieService;

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

import java.util.List;
import java.util.UUID;

@WebMvcTest({MovieController.class, GenreController.class})
@Import({
    MovieSecurityConfig.class,
    SecurityConfiguration.class,
    ServletSecurityConfiguration.class
})
class MovieIndependentResourceServerTest {

    private static final TestJwtIssuer JWT_ISSUER =
            new TestJwtIssuer("https://identity.movie.test", "cinema-api");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MovieService movieService;

    @MockitoBean private GenreService genreService;

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
    void validJwtShouldAuthorizeMovieMutation() throws Exception {

        UUID movieId = UUID.randomUUID();

        String token =
                JWT_ISSUER.token(
                        claims ->
                                claims.claim("roles", List.of("STAFF"))
                                        .claim("permissions", List.of("movie:manage")));

        mockMvc.perform(
                        delete("/api/v1/movies/{movieId}", movieId)
                                .header(HttpHeaders.AUTHORIZATION, JWT_ISSUER.bearer(token)))
                .andExpect(status().isNoContent());

        verify(movieService).delete(movieId);
    }

    @Test
    void untrustedJwtShouldBeRejectedByMovieService() throws Exception {

        UUID movieId = UUID.randomUUID();

        String token =
                JWT_ISSUER.untrustedToken(
                        claims -> claims.claim("permissions", List.of("movie:manage")));

        assertUnauthorized(movieId, token);

        verifyNoInteractions(movieService);
    }

    @Test
    void jwtWithWrongAudienceShouldBeRejectedByMovieService() throws Exception {

        UUID movieId = UUID.randomUUID();

        String token =
                JWT_ISSUER.token(
                        claims ->
                                claims.audience("untrusted-api")
                                        .claim("permissions", List.of("movie:manage")));

        assertUnauthorized(movieId, token);

        verifyNoInteractions(movieService);
    }

    @Test
    void forwardedIdentityHeadersShouldNotAuthenticateMovieRequest() throws Exception {

        UUID movieId = UUID.randomUUID();

        mockMvc.perform(
                        delete("/api/v1/movies/{movieId}", movieId)
                                .header("X-User-Id", UUID.randomUUID().toString())
                                .header("X-Roles", "ADMIN")
                                .header("X-Permissions", "movie:manage"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("SECURITY_AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(movieService);
    }

    private void assertUnauthorized(UUID movieId, String token) throws Exception {

        mockMvc.perform(
                        delete("/api/v1/movies/{movieId}", movieId)
                                .header(HttpHeaders.AUTHORIZATION, JWT_ISSUER.bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.error.category").value("SECURITY"));
    }
}
