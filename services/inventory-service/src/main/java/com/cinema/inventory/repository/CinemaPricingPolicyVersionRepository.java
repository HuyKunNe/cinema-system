package com.cinema.inventory.repository;

import com.cinema.inventory.entity.CinemaPricingPolicyVersion;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CinemaPricingPolicyVersionRepository
        extends JpaRepository<CinemaPricingPolicyVersion, UUID> {

    @EntityGraph(attributePaths = {"policy", "policy.cinema"})
    Optional<CinemaPricingPolicyVersion> findByPolicy_IdAndRevision(UUID policyId, long revision);

    @EntityGraph(attributePaths = {"policy", "policy.cinema"})
    List<CinemaPricingPolicyVersion> findAllByPolicy_IdOrderByRevisionDesc(UUID policyId);

    @Query(
            """
            select revision
            from CinemaPricingPolicyVersion revision
            join fetch revision.policy policy
            join fetch policy.cinema cinema
            where cinema.id = :cinemaId
              and revision.revision = (
                  select max(candidate.revision)
                  from CinemaPricingPolicyVersion candidate
                  where candidate.policy.id = policy.id
              )
            order by revision.name asc, policy.id asc
            """)
    List<CinemaPricingPolicyVersion> findLatestForCinema(@Param("cinemaId") UUID cinemaId);

    @Query(
            """
            select revision
            from CinemaPricingPolicyVersion revision
            join fetch revision.policy policy
            join fetch policy.cinema cinema
            where revision.id = :versionId
              and policy.id = :policyId
              and cinema.id = :cinemaId
            """)
    Optional<CinemaPricingPolicyVersion> findForRead(
            @Param("cinemaId") UUID cinemaId,
            @Param("policyId") UUID policyId,
            @Param("versionId") UUID versionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select revision
            from CinemaPricingPolicyVersion revision
            where revision.id = :versionId
              and revision.policy.id = :policyId
            """)
    Optional<CinemaPricingPolicyVersion> findForUpdate(
            @Param("policyId") UUID policyId, @Param("versionId") UUID versionId);
}
