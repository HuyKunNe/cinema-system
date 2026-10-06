package com.cinema.inventory.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.ReplaceRoomLayoutContentRequest;
import com.cinema.inventory.dto.response.RoomLayoutContentResponse;
import com.cinema.inventory.dto.response.RoomLayoutContentResponse
        .RoomLayoutElementPositionResponse;
import com.cinema.inventory.dto.response.RoomLayoutContentResponse
        .RoomLayoutGeometryResponse;
import com.cinema.inventory.dto.response.RoomLayoutContentResponse
        .RoomLayoutSeatPositionResponse;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.entity.RoomLayoutElement;
import com.cinema.inventory.entity.RoomLayoutGeometry;
import com.cinema.inventory.entity.RoomLayoutSeat;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.RoomLayoutMapper;
import com.cinema.inventory.repository.RoomLayoutElementRepository;
import com.cinema.inventory.repository.RoomLayoutRepository;
import com.cinema.inventory.repository.RoomLayoutSeatRepository;
import com.cinema.inventory.repository.SeatRepository;

@Service
public class RoomLayoutContentService {

    private final RoomLayoutRepository layoutRepository;
    private final RoomLayoutSeatRepository layoutSeatRepository;
    private final RoomLayoutElementRepository layoutElementRepository;
    private final SeatRepository seatRepository;
    private final RoomLayoutMapper layoutMapper;

    public RoomLayoutContentService(
            RoomLayoutRepository layoutRepository,
            RoomLayoutSeatRepository layoutSeatRepository,
            RoomLayoutElementRepository layoutElementRepository,
            SeatRepository seatRepository,
            RoomLayoutMapper layoutMapper) {

        this.layoutRepository = layoutRepository;
        this.layoutSeatRepository = layoutSeatRepository;
        this.layoutElementRepository = layoutElementRepository;
        this.seatRepository = seatRepository;
        this.layoutMapper = layoutMapper;
    }

    @Transactional
    public RoomLayoutContentResponse getContent(UUID layoutId) {

        RoomLayout layout = layoutRepository
                .findByIdForRead(layoutId)
                .orElseThrow(() -> new NotFoundException(
                        InventoryErrorCode.ROOM_LAYOUT_NOT_FOUND));

        return readContent(layout);
    }

    @Transactional
    public RoomLayoutContentResponse replaceContent(
            UUID layoutId,
            ReplaceRoomLayoutContentRequest request) {

        RoomLayout layout = layoutRepository
                .findByIdForUpdate(layoutId)
                .orElseThrow(() -> new NotFoundException(
                        InventoryErrorCode.ROOM_LAYOUT_NOT_FOUND));

        layout.requireDraft();

        if (!request.expectedVersion().equals(layout.getVersion())) {
            throw new ConflictException(
                    InventoryErrorCode.ROOM_LAYOUT_VERSION_CONFLICT);
        }

        Set<UUID> seatIds = new HashSet<>();

        for (var input : request.seats()) {
            if (!seatIds.add(input.seatId())) {
                throw new ValidationException(
                        InventoryErrorCode.ROOM_LAYOUT_DUPLICATE_SEAT);
            }
        }

        Map<UUID, Seat> seatsById = seatRepository
                .findAllById(seatIds)
                .stream()
                .collect(Collectors.toMap(
                        Seat::getId,
                        Function.identity()));

        List<RoomLayoutSeat> newSeats = request.seats()
                .stream()
                .map(input -> {
                    Seat seat = seatsById.get(input.seatId());

                    if (seat == null) {
                        throw new NotFoundException(
                                InventoryErrorCode.SEAT_NOT_FOUND);
                    }

                    return new RoomLayoutSeat(
                            layout,
                            seat,
                            input.geometry().toGeometry());
                })
                .toList();

        List<RoomLayoutElement> newElements = request.elements()
                .stream()
                .map(input -> new RoomLayoutElement(
                        layout,
                        input.kind(),
                        input.label(),
                        input.geometry().toGeometry()))
                .toList();

        // All inputs have been validated before deleting old content.
        layout.markContentChanged();

        layoutSeatRepository.deleteAllForLayout(layoutId);
        layoutElementRepository.deleteAllForLayout(layoutId);

        layoutSeatRepository.saveAllAndFlush(newSeats);
        layoutElementRepository.saveAllAndFlush(newElements);

        layoutRepository.flush();

        return readContent(layout);
    }

    private RoomLayoutContentResponse readContent(RoomLayout layout) {

        List<RoomLayoutSeatPositionResponse> seats =
                layoutSeatRepository
                        .findAllByLayout_IdOrderBySeatNumberSnapshotAsc(
                                layout.getId())
                        .stream()
                        .map(position -> new RoomLayoutSeatPositionResponse(
                                position.getId(),
                                position.getSeat().getId(),
                                position.getSeatNumberSnapshot(),
                                position.getRowLabelSnapshot(),
                                position.getSeatTypeSnapshot(),
                                mapGeometry(position.getGeometry())))
                        .toList();

        List<RoomLayoutElementPositionResponse> elements =
                layoutElementRepository
                        .findAllByLayout_IdOrderByIdAsc(layout.getId())
                        .stream()
                        .map(element -> new RoomLayoutElementPositionResponse(
                                element.getId(),
                                element.getKind(),
                                element.getLabel(),
                                mapGeometry(element.getGeometry())))
                        .toList();

        return new RoomLayoutContentResponse(
                layoutMapper.toResponse(layout),
                seats,
                elements);
    }

    private RoomLayoutGeometryResponse mapGeometry(
            RoomLayoutGeometry geometry) {

        return new RoomLayoutGeometryResponse(
                geometry.getX(),
                geometry.getY(),
                geometry.getWidth(),
                geometry.getHeight(),
                geometry.getRotationDegrees());
    }
}
