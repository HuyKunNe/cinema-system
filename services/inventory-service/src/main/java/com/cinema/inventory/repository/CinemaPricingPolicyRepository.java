package com.cinema.inventory.repository;

import com.cinema.inventory.entity.CinemaPricingPolicy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CinemaPricingPolicyRepository extends JpaRepository<CinemaPricingPolicy, UUID> {

    List<CinemaPricingPolicy> findAllByCinema_IdOrderByPolicyVersionDesc(UUID cinemaId);

    Optional<CinemaPricingPolicy> findByIdAndCinema_Id(UUID policyId, UUID cinemaId);

    @Query(
            """
            select coalesce(max(policy.policyVersion), 0)
            from CinemaPricingPolicy policy
            where policy.cinema.id = :cinemaId
            """)
    long findMaximumPolicyVersionByCinemaId(@Param("cinemaId") UUID cinemaId);
}
