package com.cinema.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cinema.user.entity.EmailVerificationToken;

import jakarta.persistence.LockModeType;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    List<EmailVerificationToken> findAllByUser_IdAndUsedAtIsNullAndRevokedAtIsNull(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from EmailVerificationToken token
            where token.tokenHash = :tokenHash
            """)
    Optional<EmailVerificationToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash);

    boolean existsByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from EmailVerificationToken token
            where token.user.id = :userId
              and token.usedAt is null
              and token.revokedAt is null
            """)
    List<EmailVerificationToken> findAllActiveByUserIdForUpdate(
            @Param("userId") UUID userId);
}
