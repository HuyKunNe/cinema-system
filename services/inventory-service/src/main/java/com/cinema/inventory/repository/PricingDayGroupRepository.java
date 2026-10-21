package com.cinema.inventory.repository;

import com.cinema.inventory.entity.PricingDayGroup;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PricingDayGroupRepository
        extends JpaRepository<PricingDayGroup, UUID> {

    @EntityGraph(attributePaths = "daysOfWeek")
    List<PricingDayGroup> findAllByPolicy_IdOrderByCodeAsc(UUID policyId);

    @Modifying(flushAutomatically = true)
    @Query("delete from PricingDayGroup dayGroup where dayGroup.policy.id = :policyId")
    int deleteAllForPolicy(@Param("policyId") UUID policyId);
}
