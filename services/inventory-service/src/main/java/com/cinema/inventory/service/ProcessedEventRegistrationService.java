package com.cinema.inventory.service;

import java.util.UUID;

public interface ProcessedEventRegistrationService {

    boolean register(UUID eventId, String consumerName, String eventType, String eventVersion);
}
