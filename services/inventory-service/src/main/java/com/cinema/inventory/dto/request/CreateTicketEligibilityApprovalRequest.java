package com.cinema.inventory.dto.request;

import com.cinema.common.validation.annotation.UuidV5OrV7;
import com.cinema.common.validation.annotation.UuidV7;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateTicketEligibilityApprovalRequest(
        @NotNull @UuidV7 UUID userId,

        @NotNull @UuidV5OrV7 UUID showtimeId,

        @NotNull @UuidV5OrV7 UUID showSeatId,

        @NotBlank
        @Pattern(regexp = "[A-Z][A-Z0-9_]{0,63}")
        String ticketCategoryCode,

        @NotBlank @Size(max = 150)
        String attendeeName,

        @NotBlank @Size(max = 120)
        String verificationReference,

        @NotNull
        OffsetDateTime validUntil) {}
