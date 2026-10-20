package com.cinema.inventory.repository;

import com.cinema.inventory.entity.TicketSurchargeRule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TicketSurchargeRuleRepository
        extends JpaRepository<TicketSurchargeRule, UUID> {

    List<TicketSurchargeRule> findAllByPolicy_IdOrderByPriorityDescCodeAsc(
            UUID policyId);

    @Modifying(flushAutomatically = true)
    @Query("delete from TicketSurchargeRule rule where rule.policy.id = :policyId")
    int deleteAllForPolicy(@Param("policyId") UUID policyId);
}
