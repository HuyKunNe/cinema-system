package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
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
    private final RoomRepository roomRepository;
    private final RoomLayoutPublicationValidator validator;
    private final RoomLayoutMapper mapper;
    private final Clock clock;
    private final RoomLayoutSeatBindingService bindingService;

    public RoomLayoutPublicationService(
            RoomLayoutRepository layoutRepository,
            RoomLayoutSeatRepository seatRepository,
            RoomLayoutElementRepository elementRepository,
            RoomRepository roomRepository,
            RoomLayoutPublicationValidator validator,
            RoomLayoutMapper mapper,
            Clock clock,
            RoomLayoutSeatBindingService bindingService) {

        this.layoutRepository = layoutRepository;
        this.seatRepository = seatRepository;
        this.elementRepository = elementRepository;
        this.roomRepository = roomRepository;
        this.validator = validator;
        this.mapper = mapper;
        this.clock = clock;
        this.bindingService = bindingService;
    }

    @Transactional
    public RoomLayoutResponse publish(UUID layoutId, PublishRoomLayoutRequest request) {

        UUID roomId =
                layoutRepository
                        .findRoomIdByLayoutId(layoutId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.ROOM_LAYOUT_NOT_FOUND));

        roomRepository
                .findByIdForUpdate(roomId)
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
            throw new ConflictException(InventoryErrorCode.ROOM_LAYOUT_VERSION_CONFLICT);
        }

        if (layout.isMatrixLayout()) {
            bindingService.bindForPublication(layout);
        } else {
            validator.validate(
                    layout,
                    seatRepository.findAllByLayout_IdOrderBySeatNumberSnapshotAsc(layoutId),
                    elementRepository.findAllByLayout_IdOrderByIdAsc(layoutId));
        }

        layout.publish(OffsetDateTime.now(clock));
        layoutRepository.flush();

        return mapper.toResponse(layout);
    }
}
