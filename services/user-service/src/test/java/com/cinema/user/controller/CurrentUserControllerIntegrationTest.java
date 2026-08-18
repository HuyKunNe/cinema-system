package com.cinema.user.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.dto.response.CurrentUserProfileResponse;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.security.CinemaUserDetails;
import com.cinema.user.service.UserProfileService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@SpringBootTest(
        properties = {
            "spring.main.web-application-type=servlet",
            "cinema.user.authorization-server.issuer=http://localhost:8082"
        })
@AutoConfigureMockMvc
class CurrentUserControllerIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("019c9000-0000-7000-8000-000000000001");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserProfileService userProfileService;

    @Test
    void authenticatedUserShouldReadOwnProfile() throws Exception {
        CurrentUserProfileResponse response =
                new CurrentUserProfileResponse(
                        USER_ID,
                        "member@example.com",
                        "member",
                        AccountStatus.ACTIVE,
                        "Cinema",
                        "Member",
                        "+84901234567",
                        OffsetDateTime.parse("2026-08-18T01:30:00Z"),
                        OffsetDateTime.parse("2026-08-18T01:00:00Z"),
                        OffsetDateTime.parse("2026-08-18T02:00:00Z"));

        when(userProfileService.getCurrentProfile(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me").with(user(authenticatedPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.username").value("member"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.firstName").value("Cinema"))
                .andExpect(jsonPath("$.lastName").value("Member"))
                .andExpect(jsonPath("$.phoneNumber").value("+84901234567"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.credentials").doesNotExist());

        verify(userProfileService).getCurrentProfile(USER_ID);
    }

    @Test
    void unauthenticatedRequestShouldUseLoginEntryPoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/login")));
    }

    private static CinemaUserDetails authenticatedPrincipal() {
        return new CinemaUserDetails(
                USER_ID,
                "member",
                "encoded-password",
                AccountStatus.ACTIVE,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
