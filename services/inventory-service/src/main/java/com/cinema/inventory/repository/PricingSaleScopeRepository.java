package com.cinema.inventory.repository;

import com.cinema.inventory.entity.PricingSaleScope;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PricingSaleScopeRepository extends JpaRepository<PricingSaleScope, UUID> {

    List<PricingSaleScope> findAllByPolicy_IdOrderByCodeAsc(UUID policyId);

    @Modifying(flushAutomatically = true)
    @Query("delete from PricingSaleScope scope where scope.policy.id = :policyId")
    int deleteAllForPolicy(@Param("policyId") UUID policyId);
}
