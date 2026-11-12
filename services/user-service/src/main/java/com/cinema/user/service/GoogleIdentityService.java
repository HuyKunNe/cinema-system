package com.cinema.user.service;

import java.util.UUID;

public interface GoogleIdentityService {

    GoogleAccount resolveOrCreate(
            String issuer,
            String subject,
            String email,
            Boolean emailVerified,
            String firstName,
            String lastName);

    record GoogleAccount(UUID userId, String username) {}
}
