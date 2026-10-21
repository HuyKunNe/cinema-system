package com.cinema.inventory.repository;

import com.cinema.inventory.entity.FareRule;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying(flushAutomatically = true)
    @Query("delete from FareRule rule where rule.policy.id = :policyId")
    int deleteAllForPolicy(@Param("policyId") UUID policyId);
}
