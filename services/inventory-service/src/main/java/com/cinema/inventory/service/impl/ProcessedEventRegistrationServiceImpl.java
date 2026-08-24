package com.cinema.inventory.service.impl;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.code.ErrorCode;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.ProcessedEventRepository;
import com.cinema.inventory.service.ProcessedEventRegistrationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
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

        if (eventId == null) {
            throw new ValidationException(InventoryErrorCode.EVENT_ID_REQUIRED);
        }

        int insertedRows =
                processedEventRepository.insertIfAbsent(
                        UuidGenerator.next().toString(),
                        eventId.toString(),
                        requireText(consumerName, InventoryErrorCode.CONSUMER_NAME_REQUIRED),
                        requireText(eventType, InventoryErrorCode.PROCESSED_EVENT_TYPE_REQUIRED),
                        requireText(
                                eventVersion, InventoryErrorCode.PROCESSED_EVENT_VERSION_REQUIRED),
                        OffsetDateTime.now(clock));

        return insertedRows == 1;
    }

    private static String requireText(String value, ErrorCode errorCode) {

        if (value == null || value.isBlank()) {
            throw new ValidationException(errorCode);
        }

        return value.trim();
    }
}
