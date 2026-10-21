package com.cinema.inventory.repository;

import com.cinema.inventory.entity.PricingTimeBand;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PricingTimeBandRepository extends JpaRepository<PricingTimeBand, UUID> {

    List<PricingTimeBand> findAllByPolicy_IdOrderByFromMinuteAscCodeAsc(UUID policyId);
}
