package com.cinema.inventory.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketEligibilityApprovalResponse(
        UUID id,
        UUID showtimeId,
        UUID showSeatId,
        String ticketCategoryCode,
        String attendeeName,
        OffsetDateTime validUntil) {}
