package com.cinema.payment.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RejectReconciliationRequest(
        @NotBlank @Size(max = 500) String reason, @NotNull UUID correlationId) {}
