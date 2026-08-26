package com.cinema.payment.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.common.security.config.SecurityConfiguration;
import com.cinema.common.security.config.ServletSecurityConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@WebMvcTest
@ActiveProfiles("test")
@Import({
    PaymentSecurityConfig.class,
    SecurityConfiguration.class,
    ServletSecurityConfiguration.class,
    PaymentSecurityIntegrationTest.PaymentSecurityProbeController.class
})
class PaymentSecurityIntegrationTest {

    private static final UUID PAYMENT_ID = UUID.fromString("019c1234-2222-7abc-8def-0123456789ab");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtDecoder jwtDecoder;

    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void unauthenticatedPaymentQueryShouldBeRejected() throws Exception {

        mockMvc.perform(get("/api/v1/payments/{paymentId}", PAYMENT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void paymentQueryWithoutPaymentReadShouldBeForbidden() throws Exception {

        mockMvc.perform(
                        get("/api/v1/payments/{paymentId}", PAYMENT_ID)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "booking:read"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_ACCESS_DENIED"));
    }

    @Test
    void paymentQueryWithPaymentReadShouldBeAllowed() throws Exception {

        mockMvc.perform(
                        get("/api/v1/payments/{paymentId}", PAYMENT_ID)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "payment:read"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void refundShouldRemainDeniedBeforeR27PointTen() throws Exception {

        mockMvc.perform(
                        post("/api/v1/payments/{paymentId}/refunds", PAYMENT_ID)
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "payment:read"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SECURITY_ACCESS_DENIED"));
    }

    @RestController
    @RequestMapping("/api/v1/payments")
    static class PaymentSecurityProbeController {

        @GetMapping("/{paymentId}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void findById(@PathVariable UUID paymentId) {}

        @PostMapping("/{paymentId}/refunds")
        void refund(@PathVariable UUID paymentId) {}
    }
}
