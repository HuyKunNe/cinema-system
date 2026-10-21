package com.cinema.inventory.repository;

import com.cinema.inventory.entity.PricingDayGroup;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PricingDayGroupRepository extends JpaRepository<PricingDayGroup, UUID> {

    @EntityGraph(attributePaths = "daysOfWeek")
    List<PricingDayGroup> findAllByPolicy_IdOrderByCodeAsc(UUID policyId);
}
