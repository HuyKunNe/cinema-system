package com.cinema.inventory.repository;

import com.cinema.inventory.entity.PricingQuote;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PricingQuoteRepository extends JpaRepository<PricingQuote, UUID> {

    Optional<PricingQuote> findByIdAndUserId(UUID id, UUID userId);

    @Query(
            """
            select quote.showtimeId
            from PricingQuote quote
            where quote.id = :quoteId
              and quote.userId = :userId
            """)
    Optional<UUID> findOwnedShowtimeId(
            @Param("quoteId") UUID quoteId, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select quote
            from PricingQuote quote
            where quote.id = :quoteId
              and quote.userId = :userId
            """)
    Optional<PricingQuote> findOwnedForUpdate(
            @Param("quoteId") UUID quoteId, @Param("userId") UUID userId);
}
