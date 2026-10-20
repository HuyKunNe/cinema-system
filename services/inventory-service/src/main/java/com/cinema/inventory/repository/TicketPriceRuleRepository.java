package com.cinema.inventory.repository;

import com.cinema.inventory.entity.TicketPriceRule;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TicketPriceRuleRepository
        extends JpaRepository<TicketPriceRule, UUID> {

    @EntityGraph(attributePaths = "daysOfWeek")
    List<TicketPriceRule>
            findAllByPolicy_IdOrderByPriorityDescCodeAscTicketCategoryAsc(
                    UUID policyId);

    @Modifying(flushAutomatically = true)
    @Query("delete from TicketPriceRule rule where rule.policy.id = :policyId")
    int deleteAllForPolicy(@Param("policyId") UUID policyId);
}
