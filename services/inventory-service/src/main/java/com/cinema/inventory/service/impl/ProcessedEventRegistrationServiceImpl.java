package com.cinema.inventory.service.impl;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.inventory.repository.ProcessedEventRepository;
import com.cinema.inventory.service.ProcessedEventRegistrationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class ProcessedEventRegistrationServiceImpl implements ProcessedEventRegistrationService {

    private final ProcessedEventRepository processedEventRepository;

    private final Clock clock;

    public ProcessedEventRegistrationServiceImpl(
            ProcessedEventRepository processedEventRepository, Clock clock) {

        this.processedEventRepository = processedEventRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean register(
            UUID eventId, String consumerName, String eventType, String eventVersion) {

        Objects.requireNonNull(eventId, "eventId must not be null");

        int insertedRows =
                processedEventRepository.insertIfAbsent(
                        UuidGenerator.next().toString(),
                        eventId.toString(),
                        requireText(consumerName, "consumerName"),
                        requireText(eventType, "eventType"),
                        requireText(eventVersion, "eventVersion"),
                        OffsetDateTime.now(clock));

        return insertedRows == 1;
    }

    private static String requireText(String value, String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value.trim();
    }
}
