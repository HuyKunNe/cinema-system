package com.cinema.user.repository;

import com.cinema.user.entity.StaffCinemaAssignment;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StaffCinemaAssignmentRepository
        extends JpaRepository<StaffCinemaAssignment, UUID> {

    @EntityGraph(attributePaths = "user")
    Optional<StaffCinemaAssignment> findByUser_Id(UUID userId);

    @EntityGraph(attributePaths = "user")
    Page<StaffCinemaAssignment> findAllByCinemaId(UUID cinemaId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<StaffCinemaAssignment> findAll(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from StaffCinemaAssignment a where a.user.id = :userId")
    Optional<StaffCinemaAssignment> findByUserIdForUpdate(@Param("userId") UUID userId);
}
