package com.cinema.user.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.service.UserAccountLifecycleService;

@SpringBootTest(
        properties = {
            "spring.main.web-application-type=servlet",
            "cinema.user.authorization-server.issuer=http://localhost:8082"
        })
@AutoConfigureMockMvc
class AdministrativeUserAccountControllerIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final UUID USER_ID = UUID.fromString(
            "019ca000-0000-7000-8000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAccountLifecycleService userAccountLifecycleService;

    @Test
    @WithMockUser(authorities = "user:manage")
    void userManagerShouldPerformAllAccountLifecycleTransitions()
            throws Exception {

        mockMvc.perform(patch(path("lock")).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch(path("unlock")).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch(path("disable")).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch(path("enable")).with(csrf()))
                .andExpect(status().isNoContent());

        verify(userAccountLifecycleService).lock(USER_ID);
        verify(userAccountLifecycleService).unlock(USER_ID);
        verify(userAccountLifecycleService).disable(USER_ID);
        verify(userAccountLifecycleService).enable(USER_ID);
    }

    @Test
    @WithMockUser(authorities = "inventory:manage")
    void unrelatedAuthorityShouldBeForbidden() throws Exception {

        mockMvc.perform(patch(path("lock")).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userAccountLifecycleService);
    }

    @Test
    @WithMockUser(authorities = "user:manage")
    void stateTransitionShouldRequireCsrf() throws Exception {

        mockMvc.perform(patch(path("disable")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userAccountLifecycleService);
    }

    @Test
    void unauthenticatedRequestShouldUseLoginEntryPoint()
            throws Exception {

        mockMvc.perform(patch(path("lock")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        header().string(
                                "Location",
                                endsWith("/login")));

        verifyNoInteractions(userAccountLifecycleService);
    }

    private static String path(String transition) {

        return "/api/v1/users/"
                + USER_ID
                + "/"
                + transition;
    }
}
