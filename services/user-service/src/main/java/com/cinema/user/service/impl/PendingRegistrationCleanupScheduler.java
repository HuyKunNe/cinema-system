package com.cinema.user.service.impl;

import com.cinema.user.repository.PendingUserRegistrationRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class PendingRegistrationCleanupScheduler {

    private final PendingUserRegistrationRepository pendingRegistrationRepository;
    private final Clock clock;

    public PendingRegistrationCleanupScheduler(
            PendingUserRegistrationRepository pendingRegistrationRepository, Clock clock) {
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${USER_PENDING_REGISTRATION_CLEANUP_INTERVAL:PT1H}")
    @Transactional
    public void deleteExpiredPendingRegistrations() {
        pendingRegistrationRepository.deleteByExpiresAtLessThanEqual(
                OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }
}
