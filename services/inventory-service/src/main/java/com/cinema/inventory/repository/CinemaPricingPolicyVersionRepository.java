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

    @EntityGraph(attributePaths = {"policy", "cinemas"})
    Optional<CinemaPricingPolicyVersion> findByPolicy_IdAndRevision(UUID policyId, long revision);

    @Query(
            """
            select distinct revision
            from CinemaPricingPolicyVersion revision
            join fetch revision.policy policy
            join fetch revision.cinemas
            where revision.revision = (
                select max(candidate.revision)
                from CinemaPricingPolicyVersion candidate
                join candidate.cinemas applicable
                where candidate.policy.id = policy.id
                  and applicable.id = :cinemaId
            )
            order by revision.name asc, policy.id asc
            """)
    List<CinemaPricingPolicyVersion> findLatestForCinema(@Param("cinemaId") UUID cinemaId);

    @Query(
            """
            select distinct revision
            from CinemaPricingPolicyVersion revision
            join fetch revision.policy policy
            join fetch revision.cinemas
            where policy.id = :policyId
              and exists (
                  select candidate.id
                  from CinemaPricingPolicyVersion candidate
                  join candidate.cinemas applicable
                  where candidate.id = revision.id
                    and applicable.id = :cinemaId
              )
            order by revision.revision desc
            """)
    List<CinemaPricingPolicyVersion> findAllForCinema(
            @Param("cinemaId") UUID cinemaId, @Param("policyId") UUID policyId);

    @Query(
            """
            select distinct revision
            from CinemaPricingPolicyVersion revision
            join fetch revision.policy policy
            join fetch revision.cinemas
            where revision.id = :versionId
              and policy.id = :policyId
              and exists (
                  select candidate.id
                  from CinemaPricingPolicyVersion candidate
                  join candidate.cinemas applicable
                  where candidate.id = revision.id
                    and applicable.id = :cinemaId
              )
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

    @EntityGraph(attributePaths = {"policy", "cinemas"})
    Optional<CinemaPricingPolicyVersion> findByPolicy_IdAndDraftClientRequestId(
            UUID policyId, UUID draftClientRequestId);

    @Query(
            """
            select coalesce(max(revision.revision), 0)
            from CinemaPricingPolicyVersion revision
            where revision.policy.id = :policyId
            """)
    long findHighestRevision(@Param("policyId") UUID policyId);
}
