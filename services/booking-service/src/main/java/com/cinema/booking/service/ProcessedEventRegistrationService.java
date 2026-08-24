package com.cinema.booking.service;

import java.util.UUID;

public interface ProcessedEventRegistrationService {

    boolean register(UUID eventId, String consumerName, String eventType, String eventVersion);
}
