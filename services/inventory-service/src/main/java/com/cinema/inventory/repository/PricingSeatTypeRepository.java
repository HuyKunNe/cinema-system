package com.cinema.inventory.repository;

import com.cinema.inventory.entity.PricingSeatType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PricingSeatTypeRepository
        extends JpaRepository<PricingSeatType, UUID> {

    List<PricingSeatType> findAllByPolicy_IdOrderByCodeAsc(UUID policyId);

    @Modifying(flushAutomatically = true)
    @Query(
            """
            delete from PricingSeatType seatType
            where seatType.policy.id = :policyId
            """)
    int deleteAllForPolicy(@Param("policyId") UUID policyId);
}
