package com.cinema.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.request.CreateRoomRequest;
import com.cinema.inventory.dto.request.UpdateRoomRequest;
import com.cinema.inventory.dto.response.RoomResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.mapper.RoomMapper;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    private static final UUID CINEMA_ID = UUID.fromString(
            "019102b2-7c00-7000-8000-000000000001");

    private static final UUID ROOM_ID = UUID.fromString(
            "019102b2-7c00-7000-8000-000000000002");

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private CinemaRepository cinemaRepository;

    @Mock
    private RoomMapper roomMapper;

    private RoomServiceImpl roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomServiceImpl(
                roomRepository,
                cinemaRepository,
                roomMapper);
    }

    @Test
    void createShouldNormalizeSaveAndReturnResponse() {
        Cinema cinema = activeCinema();

        CreateRoomRequest request = new CreateRoomRequest(
                "  Room 01  ",
                RoomType.IMAX);

        RoomResponse expectedResponse = response(
                "Room 01",
                RoomType.IMAX,
                true);

        when(cinemaRepository.findById(CINEMA_ID))
                .thenReturn(Optional.of(cinema));

        when(roomRepository
                .existsByCinema_IdAndNameIgnoreCase(
                        CINEMA_ID,
                        "Room 01"))
                .thenReturn(false);

        when(roomRepository.save(any(Room.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(roomMapper.toResponse(any(Room.class)))
                .thenReturn(expectedResponse);

        RoomResponse result = roomService.create(
                CINEMA_ID,
                request);

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);

        verify(roomRepository)
                .save(roomCaptor.capture());

        Room savedRoom = roomCaptor.getValue();

        assertThat(savedRoom.getCinema())
                .isSameAs(cinema);

        assertThat(savedRoom.getName())
                .isEqualTo("Room 01");

        assertThat(savedRoom.getRoomType())
                .isEqualTo(RoomType.IMAX);

        assertThat(savedRoom.isActive())
                .isTrue();

        verify(roomMapper)
                .toResponse(savedRoom);

        assertThat(result)
                .isSameAs(expectedResponse);
    }

    @Test
    void createShouldThrowWhenCinemaDoesNotExist() {
        CreateRoomRequest request = new CreateRoomRequest(
                "Room 01",
                RoomType.STANDARD);

        when(cinemaRepository.findById(CINEMA_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.create(
                CINEMA_ID,
                request))
                .isInstanceOf(NotFoundException.class);

        verify(cinemaRepository)
                .findById(CINEMA_ID);

        verifyNoInteractions(
                roomRepository,
                roomMapper);
    }

    @Test
    void createShouldThrowWhenCinemaIsInactive() {
        Cinema cinema = activeCinema();
        cinema.deactivate();

        CreateRoomRequest request = new CreateRoomRequest(
                "Room 01",
                RoomType.STANDARD);

        when(cinemaRepository.findById(CINEMA_ID))
                .thenReturn(Optional.of(cinema));

        assertThatThrownBy(() -> roomService.create(
                CINEMA_ID,
                request))
                .isInstanceOf(ConflictException.class);

        verify(cinemaRepository)
                .findById(CINEMA_ID);

        verify(roomRepository, never())
                .existsByCinema_IdAndNameIgnoreCase(
                        any(),
                        any());

        verify(roomRepository, never())
                .save(any(Room.class));

        verifyNoInteractions(roomMapper);
    }

    @Test
    void createShouldThrowWhenRoomNameAlreadyExists() {
        Cinema cinema = activeCinema();

        CreateRoomRequest request = new CreateRoomRequest(
                "  Room 01  ",
                RoomType.STANDARD);

        when(cinemaRepository.findById(CINEMA_ID))
                .thenReturn(Optional.of(cinema));

        when(roomRepository
                .existsByCinema_IdAndNameIgnoreCase(
                        CINEMA_ID,
                        "Room 01"))
                .thenReturn(true);

        assertThatThrownBy(() -> roomService.create(
                CINEMA_ID,
                request))
                .isInstanceOf(ConflictException.class);

        verify(roomRepository)
                .existsByCinema_IdAndNameIgnoreCase(
                        CINEMA_ID,
                        "Room 01");

        verify(roomRepository, never())
                .save(any(Room.class));

        verifyNoInteractions(roomMapper);
    }

    @Test
    void getByIdShouldReturnMappedRoom() {
        Room room = room();

        RoomResponse expectedResponse = response(
                room.getName(),
                room.getRoomType(),
                room.isActive());

        when(roomRepository.findById(ROOM_ID))
                .thenReturn(Optional.of(room));

        when(roomMapper.toResponse(room))
                .thenReturn(expectedResponse);

        RoomResponse result = roomService.getById(ROOM_ID);

        assertThat(result)
                .isSameAs(expectedResponse);

        verify(roomRepository)
                .findById(ROOM_ID);

        verify(roomMapper)
                .toResponse(room);
    }

    @Test
    void getByIdShouldThrowWhenRoomDoesNotExist() {
        when(roomRepository.findById(ROOM_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getById(ROOM_ID))
                .isInstanceOf(NotFoundException.class);

        verify(roomRepository)
                .findById(ROOM_ID);

        verifyNoInteractions(roomMapper);
    }

    @Test
    void getActiveRoomsShouldReturnMappedRooms() {
        Cinema cinema = activeCinema();
        Room room = new Room(
                cinema,
                "Room 01",
                RoomType.STANDARD);

        List<Room> rooms = List.of(room);

        List<RoomResponse> expectedResponses = List.of(response(
                "Room 01",
                RoomType.STANDARD,
                true));

        when(cinemaRepository.findById(CINEMA_ID))
                .thenReturn(Optional.of(cinema));

        when(roomRepository
                .findAllByCinema_IdAndActiveTrueOrderByNameAsc(
                        CINEMA_ID))
                .thenReturn(rooms);

        when(roomMapper.toResponses(rooms))
                .thenReturn(expectedResponses);

        List<RoomResponse> result = roomService.getActiveRooms(CINEMA_ID);

        assertThat(result)
                .isSameAs(expectedResponses);

        verify(cinemaRepository)
                .findById(CINEMA_ID);

        verify(roomRepository)
                .findAllByCinema_IdAndActiveTrueOrderByNameAsc(
                        CINEMA_ID);

        verify(roomMapper)
                .toResponses(rooms);
    }

    @Test
    void getActiveRoomsShouldThrowWhenCinemaDoesNotExist() {
        when(cinemaRepository.findById(CINEMA_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getActiveRooms(CINEMA_ID))
                .isInstanceOf(NotFoundException.class);

        verify(roomRepository, never())
                .findAllByCinema_IdAndActiveTrueOrderByNameAsc(
                        any());

        verifyNoInteractions(roomMapper);
    }

    @Test
    void updateShouldNormalizeUpdateAndDeactivateRoom() {
        Cinema cinema = org.mockito.Mockito.mock(Cinema.class);

        when(cinema.getId())
                .thenReturn(CINEMA_ID);

        Room room = new Room(
                cinema,
                "Room 01",
                RoomType.STANDARD);

        UpdateRoomRequest request = new UpdateRoomRequest(
                "  IMAX Room  ",
                RoomType.IMAX,
                false);

        RoomResponse expectedResponse = response(
                "IMAX Room",
                RoomType.IMAX,
                false);

        when(roomRepository.findById(ROOM_ID))
                .thenReturn(Optional.of(room));

        when(roomRepository
                .existsByCinema_IdAndNameIgnoreCaseAndIdNot(
                        CINEMA_ID,
                        "IMAX Room",
                        ROOM_ID))
                .thenReturn(false);

        when(roomMapper.toResponse(room))
                .thenReturn(expectedResponse);

        RoomResponse result = roomService.update(
                ROOM_ID,
                request);

        assertThat(room.getName())
                .isEqualTo("IMAX Room");

        assertThat(room.getRoomType())
                .isEqualTo(RoomType.IMAX);

        assertThat(room.isActive())
                .isFalse();

        assertThat(result)
                .isSameAs(expectedResponse);

        verify(roomRepository, never())
                .save(any(Room.class));

        verify(roomMapper)
                .toResponse(room);
    }

    @Test
    void updateShouldActivateRoomWhenRequested() {
        Cinema cinema = org.mockito.Mockito.mock(Cinema.class);

        when(cinema.getId())
                .thenReturn(CINEMA_ID);

        Room room = new Room(
                cinema,
                "Room 01",
                RoomType.STANDARD);

        room.deactivate();

        UpdateRoomRequest request = new UpdateRoomRequest(
                "Room 01",
                RoomType.VIP,
                true);

        RoomResponse expectedResponse = response(
                "Room 01",
                RoomType.VIP,
                true);

        when(roomRepository.findById(ROOM_ID))
                .thenReturn(Optional.of(room));

        when(roomRepository
                .existsByCinema_IdAndNameIgnoreCaseAndIdNot(
                        CINEMA_ID,
                        "Room 01",
                        ROOM_ID))
                .thenReturn(false);

        when(roomMapper.toResponse(room))
                .thenReturn(expectedResponse);

        RoomResponse result = roomService.update(
                ROOM_ID,
                request);

        assertThat(room.isActive())
                .isTrue();

        assertThat(room.getRoomType())
                .isEqualTo(RoomType.VIP);

        assertThat(result)
                .isSameAs(expectedResponse);
    }

    @Test
    void updateShouldThrowWhenRoomDoesNotExist() {
        UpdateRoomRequest request = new UpdateRoomRequest(
                "Room 01",
                RoomType.STANDARD,
                true);

        when(roomRepository.findById(ROOM_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.update(
                ROOM_ID,
                request))
                .isInstanceOf(NotFoundException.class);

        verify(roomRepository)
                .findById(ROOM_ID);

        verifyNoInteractions(roomMapper);
    }

    @Test
    void updateShouldThrowWhenAnotherRoomHasSameName() {
        Cinema cinema = org.mockito.Mockito.mock(Cinema.class);

        when(cinema.getId())
                .thenReturn(CINEMA_ID);

        Room room = new Room(
                cinema,
                "Room 01",
                RoomType.STANDARD);

        UpdateRoomRequest request = new UpdateRoomRequest(
                "  Room 02  ",
                RoomType.IMAX,
                true);

        when(roomRepository.findById(ROOM_ID))
                .thenReturn(Optional.of(room));

        when(roomRepository
                .existsByCinema_IdAndNameIgnoreCaseAndIdNot(
                        CINEMA_ID,
                        "Room 02",
                        ROOM_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> roomService.update(
                ROOM_ID,
                request))
                .isInstanceOf(ConflictException.class);

        assertThat(room.getName())
                .isEqualTo("Room 01");

        assertThat(room.getRoomType())
                .isEqualTo(RoomType.STANDARD);

        verify(roomMapper, never())
                .toResponse(any(Room.class));
    }

    private Cinema activeCinema() {
        return new Cinema(
                "CGV Vincom",
                "72 Le Thanh Ton",
                "Ho Chi Minh");
    }

    private Room room() {
        return new Room(
                activeCinema(),
                "Room 01",
                RoomType.STANDARD);
    }

    private RoomResponse response(
            String name,
            RoomType roomType,
            boolean active) {

        return new RoomResponse(
                ROOM_ID,
                CINEMA_ID,
                name,
                roomType,
                active,
                null,
                null);
    }
}
