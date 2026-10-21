package com.cinema.inventory.repository;

import com.cinema.inventory.entity.PricingTicketCategory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface PricingTicketCategoryRepository
        extends JpaRepository<PricingTicketCategory, UUID> {

    List<PricingTicketCategory> findAllByPolicy_IdOrderByCodeAsc(UUID policyId);

    @Modifying(flushAutomatically = true)
    @Query("delete from PricingTicketCategory category " + "where category.policy.id = :policyId")
    int deleteAllForPolicy(@Param("policyId") UUID policyId);

    Optional<PricingTicketCategory> findByPolicy_IdAndCode(UUID policyId, String code);
}
