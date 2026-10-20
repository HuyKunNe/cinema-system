package com.cinema.inventory.repository;

import com.cinema.inventory.entity.CinemaHolidayDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CinemaHolidayDateRepository
        extends JpaRepository<CinemaHolidayDate, UUID> {

    List<CinemaHolidayDate> findAllByPolicy_IdOrderByHolidayDateAsc(
            UUID policyId);

    @Modifying(flushAutomatically = true)
    @Query("delete from CinemaHolidayDate holiday where holiday.policy.id = :policyId")
    int deleteAllForPolicy(@Param("policyId") UUID policyId);
}
