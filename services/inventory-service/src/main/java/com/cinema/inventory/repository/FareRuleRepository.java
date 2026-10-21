package com.cinema.inventory.repository;

import com.cinema.inventory.entity.FareRule;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FareRuleRepository extends JpaRepository<FareRule, UUID> {

    @EntityGraph(
            attributePaths = {
                "dayGroup",
                "dayGroup.daysOfWeek",
                "timeBand"
            })
    List<FareRule> findAllByPolicy_IdOrderByPriorityDescCodeAsc(UUID policyId);
}
