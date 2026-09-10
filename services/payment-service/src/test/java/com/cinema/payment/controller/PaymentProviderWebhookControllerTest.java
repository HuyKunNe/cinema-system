package com.cinema.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.payment.provider.webhook.model.ProviderWebhookAcknowledgement;
import com.cinema.payment.service.PaymentProviderWebhookHandlingService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@WebMvcTest(PaymentProviderWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PaymentProviderWebhookControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PaymentProviderWebhookHandlingService handlingService;

    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void webhookShouldPreserveRawBodyAndReturnProviderAcknowledgement() throws Exception {

        byte[] rawBody = "{\"resultCode\":0}".getBytes(StandardCharsets.UTF_8);

        byte[] acknowledgementBody =
                "{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}"
                        .getBytes(StandardCharsets.UTF_8);

        ProviderWebhookAcknowledgement acknowledgement =
                new ProviderWebhookAcknowledgement(
                        org.springframework.http.HttpStatus.OK,
                        MediaType.APPLICATION_JSON,
                        acknowledgementBody);

        when(handlingService.handle(eq("MOMO"), org.mockito.ArgumentMatchers.anyMap(), eq(rawBody)))
                .thenReturn(acknowledgement);

        mockMvc.perform(
                        post("/api/v1/payments/webhooks/MOMO")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("X-Signature", "test-signature")
                                .content(rawBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().bytes(acknowledgementBody));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, List<String>>> headersCaptor =
                ArgumentCaptor.forClass(Map.class);

        verify(handlingService).handle(eq("MOMO"), headersCaptor.capture(), eq(rawBody));

        assertThat(headersCaptor.getValue())
                .anySatisfy(
                        (name, values) -> {
                            if (name.equalsIgnoreCase("X-Signature")) {
                                assertThat(values).containsExactly("test-signature");
                            }
                        });
    }
}
