package com.cinema.common.outbox.claim;

import com.cinema.common.outbox.entity.OutboxEventEntity;

import java.util.List;

public interface OutboxClaimService {

    List<OutboxEventEntity> claimNextBatch();
}
