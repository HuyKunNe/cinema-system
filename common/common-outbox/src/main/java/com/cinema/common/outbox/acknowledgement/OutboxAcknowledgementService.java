package com.cinema.common.outbox.acknowledgement;

import java.util.UUID;

public interface OutboxAcknowledgementService {

    boolean acknowledgeSuccess(UUID eventId, String processingOwner);

    boolean acknowledgeFailure(
            UUID eventId, String processingOwner, int currentRetryCount, Throwable exception);
}
