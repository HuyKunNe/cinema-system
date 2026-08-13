package com.cinema.user.oauth2.audit;

import java.util.UUID;

public record RevocationAuditActor(
        UUID userId,
        String name) {
}
