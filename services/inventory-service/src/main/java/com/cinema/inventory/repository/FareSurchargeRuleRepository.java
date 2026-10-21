package com.cinema.inventory.repository;

import com.cinema.inventory.entity.FareSurchargeRule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FareSurchargeRuleRepository
        extends JpaRepository<FareSurchargeRule, UUID> {

    List<FareSurchargeRule> findAllByPolicy_IdOrderByPriorityDescCodeAsc(
            UUID policyId);

    @Modifying(flushAutomatically = true)
    @Query("delete from FareSurchargeRule rule where rule.policy.id = :policyId")
    int deleteAllForPolicy(@Param("policyId") UUID policyId);
}
