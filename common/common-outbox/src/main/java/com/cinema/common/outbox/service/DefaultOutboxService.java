package com.cinema.common.outbox.service;

import org.springframework.stereotype.Service;

import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.repository.OutboxRepository;

@Service
public class DefaultOutboxService
        implements OutboxService {

    private final OutboxRepository repository;

    public DefaultOutboxService(
            OutboxRepository repository) {

        this.repository = repository;

    }

    @Override
    public void save(OutboxEventEntity event) {

        repository.save(event);

    }

}
