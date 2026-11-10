package com.cinema.inventory.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.common.api.advice.GlobalExceptionHandler;
import com.cinema.common.security.config.SecurityConfiguration;
import com.cinema.common.security.config.ServletSecurityConfiguration;
import com.cinema.inventory.config.InventorySecurityConfig;
import com.cinema.inventory.service.CinemaPricingPolicyService;
import com.cinema.inventory.service.InventoryDraftDeletionService;
import com.cinema.inventory.service.RoomLayoutContentService;
import com.cinema.inventory.service.RoomLayoutDraftCopyService;
import com.cinema.inventory.service.RoomLayoutPublicationService;
import com.cinema.inventory.service.RoomLayoutService;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.stream.Stream;

@WebMvcTest({RoomLayoutController.class, CinemaPricingPolicyController.class})
@Import({InventorySecurityConfig.class, SecurityConfiguration.class,
        ServletSecurityConfiguration.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "cinema.security.oauth2.issuer-uri=https://identity.cinema.test",
        "cinema.security.oauth2.jwk-set-uri=https://identity.cinema.test/oauth2/jwks",
        "cinema.security.oauth2.audience=cinema-api"
})
class InventoryDraftDeletionSecurityTest {

    private static final UUID ID = UUID.fromString("019a5a00-0000-7000-8000-000000000001");
    private static final String LAYOUT_URL = "/api/v1/room-layouts/" + ID;
    private static final String PRICING_URL = "/api/v1/cinemas/" + ID
            + "/pricing-policies/" + ID + "/versions/" + ID;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private InventoryDraftDeletionService deletionService;
    @MockitoBean private CinemaPricingPolicyService pricingService;
    @MockitoBean private RoomLayoutService layoutService;
    @MockitoBean private RoomLayoutContentService contentService;
    @MockitoBean private RoomLayoutDraftCopyService copyService;
    @MockitoBean private RoomLayoutPublicationService publicationService;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @ParameterizedTest
    @MethodSource("draftUrls")
    void anonymousDeletionIsRejected(String url) throws Exception {
        mockMvc.perform(delete(url).param("expectedVersion", "3"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(deletionService);
    }

    @ParameterizedTest
    @MethodSource("draftUrls")
    void deletionRequiresInventoryManagementPermission(String url) throws Exception {
        mockMvc.perform(delete(url).param("expectedVersion", "3")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(deletionService);
    }

    @ParameterizedTest
    @MethodSource("draftUrls")
    void managerCanDeleteDraftWithVersion(String url) throws Exception {
        mockMvc.perform(delete(url).param("expectedVersion", "3")
                        .with(jwt().authorities(new SimpleGrantedAuthority("inventory:manage"))))
                .andExpect(status().isNoContent());

        if (url.equals(LAYOUT_URL)) {
            verify(deletionService).deleteRoomLayoutDraft(ID, 3L);
        } else {
            verify(deletionService).deletePricingPolicyDraft(ID, ID, ID, 3L);
        }
    }

    @ParameterizedTest
    @MethodSource("draftUrls")
    void deletionRequiresExpectedVersion(String url) throws Exception {
        mockMvc.perform(delete(url)
                        .with(jwt().authorities(new SimpleGrantedAuthority("inventory:manage"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(deletionService);
    }

    private static Stream<String> draftUrls() {
        return Stream.of(LAYOUT_URL, PRICING_URL);
    }
}
