package com.cinema.inventory.repository;

import com.cinema.inventory.entity.PricingTimeBand;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PricingTimeBandRepository extends JpaRepository<PricingTimeBand, UUID> {

    List<PricingTimeBand> findAllByPolicy_IdOrderByFromMinuteAscCodeAsc(UUID policyId);

    @Modifying(flushAutomatically = true)
    @Query("delete from PricingTimeBand timeBand where timeBand.policy.id = :policyId")
    int deleteAllForPolicy(@Param("policyId") UUID policyId);
}
