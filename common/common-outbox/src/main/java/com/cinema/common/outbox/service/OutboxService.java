package com.cinema.common.outbox.service;

import com.cinema.common.outbox.entity.OutboxEventEntity;

public interface OutboxService {

    void save(OutboxEventEntity event);

}
