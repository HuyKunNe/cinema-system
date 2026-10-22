package com.cinema.inventory.repository;

import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.enums.PricingPolicyStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query(
            """
            select policy
            from CinemaPricingPolicy policy
            where policy.id = :policyId
              and policy.cinema.id = :cinemaId
            """)
    Optional<CinemaPricingPolicy> findForContentRead(
            @Param("cinemaId") UUID cinemaId, @Param("policyId") UUID policyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select policy
            from CinemaPricingPolicy policy
            where policy.id = :policyId
              and policy.cinema.id = :cinemaId
            """)
    Optional<CinemaPricingPolicy> findForContentUpdate(
            @Param("cinemaId") UUID cinemaId, @Param("policyId") UUID policyId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query(
            """
            select policy
            from CinemaPricingPolicy policy
            where policy.cinema.id = :cinemaId
              and policy.status = :status
            order by policy.policyVersion asc
            """)
    List<CinemaPricingPolicy> findByCinemaAndStatusForRead(
            @Param("cinemaId") UUID cinemaId, @Param("status") PricingPolicyStatus status);
}
