package com.cinema.user.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cinema.user.entity.RefreshTokenHistory;
import com.cinema.user.oauth2.token.RefreshTokenStatus;

import jakarta.persistence.LockModeType;

public interface RefreshTokenHistoryRepository
        extends JpaRepository<RefreshTokenHistory, UUID> {

    Optional<RefreshTokenHistory> findByTokenHash(
            String tokenHash);

    List<RefreshTokenHistory> findAllByAuthorizationId(
            String authorizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT token
            FROM RefreshTokenHistory token
            WHERE token.tokenHash = :tokenHash
            """)
    Optional<RefreshTokenHistory> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
            UPDATE RefreshTokenHistory token
            SET token.status = :revokedStatus,
                token.updatedAt = :revokedAt
            WHERE token.authorizationId = :authorizationId
              AND token.status = :activeStatus
            """)
    int revokeActiveTokensByAuthorizationId(
            @Param("authorizationId") String authorizationId,
            @Param("activeStatus") RefreshTokenStatus activeStatus,
            @Param("revokedStatus") RefreshTokenStatus revokedStatus,
            @Param("revokedAt") Instant revokedAt);
}
