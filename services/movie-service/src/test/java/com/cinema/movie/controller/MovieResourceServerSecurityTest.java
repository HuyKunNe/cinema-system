package com.cinema.movie.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.common.security.config.SecurityConfiguration;
import com.cinema.common.security.config.ServletSecurityConfiguration;
import com.cinema.movie.config.MovieSecurityConfig;
import com.cinema.movie.service.GenreService;
import com.cinema.movie.service.MovieService;

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

@WebMvcTest({MovieController.class, GenreController.class})
@Import({
    MovieSecurityConfig.class,
    SecurityConfiguration.class,
    ServletSecurityConfiguration.class
})
@TestPropertySource(
        properties = {
            "cinema.security.oauth2.issuer-uri=" + "https://identity.cinema.test",
            "cinema.security.oauth2.jwk-set-uri=" + "https://identity.cinema.test/oauth2/jwks",
            "cinema.security.oauth2.audience=cinema-api"
        })
class MovieResourceServerSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MovieService movieService;

    @MockitoBean private GenreService genreService;

    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void movieCatalogShouldBePublic() throws Exception {

        when(movieService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(movieService).findAll();
    }

    @Test
    void genreCatalogShouldBePublic() throws Exception {

        when(genreService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/genres"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(genreService).findAll();
    }

    @Test
    void movieMutationShouldReturnUnauthorizedWithoutJwt() throws Exception {

        UUID movieId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/movies/{movieId}", movieId))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.error.code").value("SECURITY_AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.error.message").value("Authentication is required"))
                .andExpect(jsonPath("$.error.category").value("SECURITY"));

        verifyNoInteractions(movieService);
    }

    @Test
    void movieMutationShouldReturnForbiddenWithoutPermission() throws Exception {

        UUID movieId = UUID.randomUUID();

        mockMvc.perform(
                        delete("/api/v1/movies/{movieId}", movieId)
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_ACCESS_DENIED"))
                .andExpect(jsonPath("$.error.message").value("Access is denied"))
                .andExpect(jsonPath("$.error.category").value("SECURITY"));

        verifyNoInteractions(movieService);
    }

    @Test
    void movieMutationShouldSucceedWithManagePermission() throws Exception {

        UUID movieId = UUID.randomUUID();

        mockMvc.perform(
                        delete("/api/v1/movies/{movieId}", movieId)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "movie:manage"))))
                .andExpect(status().isNoContent());

        verify(movieService).delete(movieId);
    }

    @Test
    void genreMutationShouldReturnUnauthorizedWithoutJwt() throws Exception {

        UUID genreId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/genres/{genreId}", genreId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(genreService);
    }

    @Test
    void genreMutationShouldReturnForbiddenWithoutPermission() throws Exception {

        UUID genreId = UUID.randomUUID();

        mockMvc.perform(
                        delete("/api/v1/genres/{genreId}", genreId)
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(genreService);
    }

    @Test
    void genreMutationShouldSucceedWithManagePermission() throws Exception {

        UUID genreId = UUID.randomUUID();

        mockMvc.perform(
                        delete("/api/v1/genres/{genreId}", genreId)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "movie:manage"))))
                .andExpect(status().isNoContent());

        verify(genreService).delete(genreId);
    }

    @Test
    void optionsRequestShouldBePublic() throws Exception {

        mockMvc.perform(options("/api/v1/movies")).andExpect(status().isOk());
    }

    @Test
    void undeclaredEndpointShouldBeForbiddenForAuthenticatedUser() throws Exception {

        mockMvc.perform(get("/internal/not-exposed").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_ACCESS_DENIED"));

        verifyNoInteractions(movieService, genreService);
    }
}
