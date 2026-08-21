package com.cinema.common.outbox.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.OutboxStatus;

public interface OutboxRepository
        extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findTop100ByStatusInOrderByCreatedAt(
            List<OutboxStatus> status);

    List<OutboxEventEntity> findByStatusInOrderByCreatedAtAsc(List<OutboxStatus> of, PageRequest of0);

}
