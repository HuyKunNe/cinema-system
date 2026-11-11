package com.cinema.inventory.service;

import com.cinema.common.exception.exception.VersionConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.request.PublishRoomLayoutRequest;
import com.cinema.inventory.dto.response.RoomLayoutResponse;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.RoomLayoutMapper;
import com.cinema.inventory.repository.RoomLayoutElementRepository;
import com.cinema.inventory.repository.RoomLayoutRepository;
import com.cinema.inventory.repository.RoomLayoutSeatRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.common.exception.exception.ConflictException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RoomLayoutPublicationService {

    private final RoomLayoutRepository layoutRepository;
    private final RoomLayoutSeatRepository seatRepository;
    private final RoomLayoutElementRepository elementRepository;
    private final RoomLayoutPublicationValidator validator;
    private final RoomLayoutMapper mapper;
    private final Clock clock;
    private final RoomLayoutSeatBindingService bindingService;
    private final RoomRepository roomRepository;

    public RoomLayoutPublicationService(
            RoomLayoutRepository layoutRepository,
            RoomLayoutSeatRepository seatRepository,
            RoomLayoutElementRepository elementRepository,
            RoomLayoutPublicationValidator validator,
            RoomLayoutMapper mapper,
            Clock clock,
            RoomLayoutSeatBindingService bindingService,
            RoomRepository roomRepository) {

        this.layoutRepository = layoutRepository;
        this.seatRepository = seatRepository;
        this.elementRepository = elementRepository;
        this.validator = validator;
        this.mapper = mapper;
        this.clock = clock;
        this.bindingService = bindingService;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public RoomLayoutResponse publish(UUID layoutId, PublishRoomLayoutRequest request) {

        UUID roomId = layoutRepository.findRoomId(layoutId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.ROOM_LAYOUT_NOT_FOUND));
        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        RoomLayout layout =
                layoutRepository
                        .findByIdForUpdate(layoutId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.ROOM_LAYOUT_NOT_FOUND));

        layout.requireDraft();

        if (!request.expectedVersion().equals(layout.getVersion())) {
            throw new VersionConflictException(
                    InventoryErrorCode.ROOM_LAYOUT_VERSION_CONFLICT, layout.getVersion());
        }

        var previous = layoutRepository.findPublishedForUpdate(roomId);
        if (previous.stream().anyMatch(item -> item.getLayoutVersion() >= layout.getLayoutVersion())) {
            throw new ConflictException(InventoryErrorCode.ROOM_LAYOUT_PUBLICATION_SUPERSEDED);
        }

        if (layout.isMatrixLayout()) {
            bindingService.bindForPublication(layout);
        } else {
            validator.validate(
                    layout,
                    seatRepository.findAllByLayout_IdOrderBySeatNumberSnapshotAsc(layoutId),
                    elementRepository.findAllByLayout_IdOrderByIdAsc(layoutId));
        }

        previous.forEach(RoomLayout::retire);
        layout.publish(OffsetDateTime.now(clock));
        layoutRepository.flush();

        return mapper.toResponse(layout);
    }
}
