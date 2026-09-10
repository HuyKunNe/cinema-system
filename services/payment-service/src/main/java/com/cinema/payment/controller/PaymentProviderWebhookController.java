package com.cinema.payment.controller;

import com.cinema.payment.provider.webhook.model.ProviderWebhookAcknowledgement;
import com.cinema.payment.service.PaymentProviderWebhookHandlingService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentProviderWebhookController {

    private final PaymentProviderWebhookHandlingService handlingService;

    public PaymentProviderWebhookController(PaymentProviderWebhookHandlingService handlingService) {

        this.handlingService = handlingService;
    }

    @PostMapping("/{provider}")
    public ResponseEntity<byte[]> handle(
            @PathVariable("provider") String provider,
            @RequestHeader HttpHeaders headers,
            @RequestBody byte[] rawBody) {

        ProviderWebhookAcknowledgement acknowledgement =
                handlingService.handle(provider, copyHeaders(headers), rawBody);

        ResponseEntity.BodyBuilder responseBuilder =
                ResponseEntity.status(acknowledgement.status());

        if (acknowledgement.contentType() != null) {
            responseBuilder.contentType(acknowledgement.contentType());
        }

        return responseBuilder.body(acknowledgement.body());
    }

    private static Map<String, List<String>> copyHeaders(HttpHeaders headers) {

        Map<String, List<String>> copiedHeaders = new LinkedHashMap<>();

        headers.forEach((name, values) -> copiedHeaders.put(name, List.copyOf(values)));

        return Map.copyOf(copiedHeaders);
    }
}
