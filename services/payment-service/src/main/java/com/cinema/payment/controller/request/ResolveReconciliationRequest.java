package com.cinema.payment.controller.request;

import com.cinema.payment.enums.ReconciliationResolution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ResolveReconciliationRequest(
        @NotNull ReconciliationResolution resolution,
        @NotBlank @Size(max = 500) String reason,
        @Size(max = 255) String providerReference,
        @Size(max = 100) String failureCode,
        @Size(max = 500) String failureMessage,
        @NotNull UUID correlationId) {}
