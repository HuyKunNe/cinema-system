package com.cinema.user.repository;

import com.cinema.user.entity.PendingUserRegistration;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PendingUserRegistrationRepository
        extends JpaRepository<PendingUserRegistration, UUID> {

    Optional<PendingUserRegistration> findByNormalizedEmail(String normalizedEmail);

    boolean existsByNormalizedEmail(String normalizedEmail);

    long deleteByExpiresAtLessThanEqual(OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select registration from PendingUserRegistration registration where"
                + " registration.tokenHash = :tokenHash")
    Optional<PendingUserRegistration> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash);
}
