package com.cinema.inventory.repository;

import com.cinema.inventory.entity.CinemaPricingPolicy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CinemaPricingPolicyRepository extends JpaRepository<CinemaPricingPolicy, UUID> {

    Optional<CinemaPricingPolicy> findByCinema_IdAndClientRequestId(
            UUID cinemaId, UUID clientRequestId);

    Optional<CinemaPricingPolicy> findByIdAndCinema_Id(UUID policyId, UUID cinemaId);
}
