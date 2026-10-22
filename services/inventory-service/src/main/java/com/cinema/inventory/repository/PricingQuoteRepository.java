package com.cinema.inventory.repository;

import com.cinema.inventory.entity.PricingQuote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PricingQuoteRepository extends JpaRepository<PricingQuote, UUID> {

    Optional<PricingQuote> findByIdAndUserId(UUID id, UUID userId);
}
