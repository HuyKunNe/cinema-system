package com.cinema.inventory.repository;

import com.cinema.inventory.entity.TicketEligibilityApproval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TicketEligibilityApprovalRepository
        extends JpaRepository<TicketEligibilityApproval, UUID> {

    Optional<TicketEligibilityApproval> findByIdAndUserId(
            UUID id,
            UUID userId);
}
