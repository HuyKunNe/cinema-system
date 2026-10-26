package com.cinema.inventory.repository;

import com.cinema.inventory.entity.CinemaPricingPolicy;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CinemaPricingPolicyRepository extends JpaRepository<CinemaPricingPolicy, UUID> {

    Optional<CinemaPricingPolicy> findByCreationCinema_IdAndClientRequestId(
            UUID cinemaId, UUID clientRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select policy
            from CinemaPricingPolicy policy
            where policy.id = :policyId
            """)
    Optional<CinemaPricingPolicy> findByIdForUpdate(@Param("policyId") UUID policyId);
}
