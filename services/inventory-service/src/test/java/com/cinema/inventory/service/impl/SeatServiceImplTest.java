package com.cinema.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import com.cinema.inventory.dto.request.CreateSeatRequest;
import com.cinema.inventory.dto.request.UpdateSeatRequest;
import com.cinema.inventory.dto.response.SeatResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.mapper.SeatMapper;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.SeatRepository;

@ExtendWith(MockitoExtension.class)
class SeatServiceImplTest {

    private static final UUID ROOM_ID = UUID.fromString(
            "019102b2-7c00-7000-8000-000000000001");

    private static final UUID SEAT_ID = UUID.fromString(
            "019102b2-7c00-7000-8000-000000000002");

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private SeatMapper seatMapper;

    private SeatServiceImpl seatService;

    @BeforeEach
    void setUp() {
        seatService = new SeatServiceImpl(
                seatRepository,
                roomRepository,
                seatMapper);
    }

    @Test
    void createShouldNormalizeSaveAndReturnResponse() {
        Room room = activeRoom();

        CreateSeatRequest request = new CreateSeatRequest(
                "  A01  ",
                "  A  ",
                SeatType.STANDARD);

        SeatResponse expectedResponse = response(
                "A01",
                "A",
                SeatType.STANDARD,
                true);

        when(roomRepository.findById(ROOM_ID))
                .thenReturn(Optional.of(room));

        when(seatRepository
                .existsByRoom_IdAndSeatNumberIgnoreCase(
                        ROOM_ID,
                        "A01"))
                .thenReturn(false);

        when(seatRepository.save(any(Seat.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(seatMapper.toResponse(any(Seat.class)))
                .thenReturn(expectedResponse);

        SeatResponse result = seatService.create(
                ROOM_ID,
                request);

        ArgumentCaptor<Seat> seatCaptor = ArgumentCaptor.forClass(Seat.class);

        verify(seatRepository)
                .save(seatCaptor.capture());

        Seat savedSeat = seatCaptor.getValue();

        assertThat(savedSeat.getRoom())
                .isSameAs(room);

        assertThat(savedSeat.getSeatNumber())
                .isEqualTo("A01");

        assertThat(savedSeat.getRowLabel())
                .isEqualTo("A");

        assertThat(savedSeat.getSeatType())
                .isEqualTo(SeatType.STANDARD);

        assertThat(savedSeat.isActive())
                .isTrue();

        verify(roomRepository)
                .findById(ROOM_ID);

        verify(seatRepository)
                .existsByRoom_IdAndSeatNumberIgnoreCase(
                        ROOM_ID,
                        "A01");

        verify(seatMapper)
                .toResponse(savedSeat);

        assertThat(result)
                .isSameAs(expectedResponse);
    }

    @Test
    void createShouldThrowWhenRoomDoesNotExist() {
        CreateSeatRequest request = new CreateSeatRequest(
                "A01",
                "A",
                SeatType.STANDARD);

        when(roomRepository.findById(ROOM_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> seatService.create(
                ROOM_ID,
                request))
                .isInstanceOf(NotFoundException.class);

        verify(roomRepository)
                .findById(ROOM_ID);

        verifyNoInteractions(
                seatRepository,
                seatMapper);
    }

    @Test
    void createShouldThrowWhenRoomIsInactive() {
        Room room = activeRoom();
        room.deactivate();

        CreateSeatRequest request = new CreateSeatRequest(
                "A01",
                "A",
                SeatType.STANDARD);

        when(roomRepository.findById(ROOM_ID))
                .thenReturn(Optional.of(room));

        assertThatThrownBy(() -> seatService.create(
                ROOM_ID,
                request))
                .isInstanceOf(ConflictException.class);

        verify(roomRepository)
                .findById(ROOM_ID);

        verify(seatRepository, never())
                .existsByRoom_IdAndSeatNumberIgnoreCase(
                        any(),
                        any());

        verify(seatRepository, never())
                .save(any(Seat.class));

        verifyNoInteractions(seatMapper);
    }

    @Test
    void createShouldThrowWhenSeatNumberAlreadyExists() {
        Room room = activeRoom();

        CreateSeatRequest request = new CreateSeatRequest(
                "  A01  ",
                "  A  ",
                SeatType.STANDARD);

        when(roomRepository.findById(ROOM_ID))
                .thenReturn(Optional.of(room));

        when(seatRepository
                .existsByRoom_IdAndSeatNumberIgnoreCase(
                        ROOM_ID,
                        "A01"))
                .thenReturn(true);

        assertThatThrownBy(() -> seatService.create(
                ROOM_ID,
                request))
                .isInstanceOf(ConflictException.class);

        verify(roomRepository)
                .findById(ROOM_ID);

        verify(seatRepository)
                .existsByRoom_IdAndSeatNumberIgnoreCase(
                        ROOM_ID,
                        "A01");

        verify(seatRepository, never())
                .save(any(Seat.class));

        verifyNoInteractions(seatMapper);
    }

    @Test
    void getByIdShouldReturnMappedSeat() {
        Seat seat = seat();

        SeatResponse expectedResponse = response(
                seat.getSeatNumber(),
                seat.getRowLabel(),
                seat.getSeatType(),
                seat.isActive());

        when(seatRepository.findById(SEAT_ID))
                .thenReturn(Optional.of(seat));

        when(seatMapper.toResponse(seat))
                .thenReturn(expectedResponse);

        SeatResponse result = seatService.getById(SEAT_ID);

        assertThat(result)
                .isSameAs(expectedResponse);

        verify(seatRepository)
                .findById(SEAT_ID);

        verify(seatMapper)
                .toResponse(seat);
    }

    @Test
    void getByIdShouldThrowWhenSeatDoesNotExist() {
        when(seatRepository.findById(SEAT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> seatService.getById(SEAT_ID))
                .isInstanceOf(NotFoundException.class);

        verify(seatRepository)
                .findById(SEAT_ID);

        verifyNoInteractions(seatMapper);
    }

    @Test
    void getActiveSeatsShouldReturnMappedSeats() {
        Room room = activeRoom();

        Seat seat = new Seat(
                room,
                "A01",
                "A",
                SeatType.STANDARD);

        List<Seat> seats = List.of(seat);

        List<SeatResponse> expectedResponses = List.of(response(
                "A01",
                "A",
                SeatType.STANDARD,
                true));

        when(roomRepository.findById(ROOM_ID))
                .thenReturn(Optional.of(room));

        when(seatRepository
                .findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(
                        ROOM_ID))
                .thenReturn(seats);

        when(seatMapper.toResponses(seats))
                .thenReturn(expectedResponses);

        List<SeatResponse> result = seatService.getActiveSeats(ROOM_ID);

        assertThat(result)
                .isSameAs(expectedResponses);

        verify(roomRepository)
                .findById(ROOM_ID);

        verify(seatRepository)
                .findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(
                        ROOM_ID);

        verify(seatMapper)
                .toResponses(seats);
    }

    @Test
    void getActiveSeatsShouldThrowWhenRoomDoesNotExist() {
        when(roomRepository.findById(ROOM_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> seatService.getActiveSeats(ROOM_ID))
                .isInstanceOf(NotFoundException.class);

        verify(roomRepository)
                .findById(ROOM_ID);

        verify(seatRepository, never())
                .findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(
                        any());

        verifyNoInteractions(seatMapper);
    }

    @Test
    void updateShouldNormalizeUpdateAndDeactivateSeat() {
        Room room = mock(Room.class);

        when(room.getId())
                .thenReturn(ROOM_ID);

        Seat seat = new Seat(
                room,
                "A01",
                "A",
                SeatType.STANDARD);

        UpdateSeatRequest request = new UpdateSeatRequest(
                "  B02  ",
                "  B  ",
                SeatType.VIP,
                false);

        SeatResponse expectedResponse = response(
                "B02",
                "B",
                SeatType.VIP,
                false);

        when(seatRepository.findById(SEAT_ID))
                .thenReturn(Optional.of(seat));

        when(seatRepository
                .existsByRoom_IdAndSeatNumberIgnoreCaseAndIdNot(
                        ROOM_ID,
                        "B02",
                        SEAT_ID))
                .thenReturn(false);

        when(seatMapper.toResponse(seat))
                .thenReturn(expectedResponse);

        SeatResponse result = seatService.update(
                SEAT_ID,
                request);

        assertThat(seat.getSeatNumber())
                .isEqualTo("B02");

        assertThat(seat.getRowLabel())
                .isEqualTo("B");

        assertThat(seat.getSeatType())
                .isEqualTo(SeatType.VIP);

        assertThat(seat.isActive())
                .isFalse();

        assertThat(result)
                .isSameAs(expectedResponse);

        verify(seatRepository)
                .findById(SEAT_ID);

        verify(seatRepository)
                .existsByRoom_IdAndSeatNumberIgnoreCaseAndIdNot(
                        ROOM_ID,
                        "B02",
                        SEAT_ID);

        verify(seatRepository, never())
                .save(any(Seat.class));

        verify(seatMapper)
                .toResponse(seat);
    }

    @Test
    void updateShouldActivateSeatWhenRequested() {
        Room room = mock(Room.class);

        when(room.getId())
                .thenReturn(ROOM_ID);

        Seat seat = new Seat(
                room,
                "A01",
                "A",
                SeatType.STANDARD);

        seat.deactivate();

        UpdateSeatRequest request = new UpdateSeatRequest(
                "  A01  ",
                "  A  ",
                SeatType.ACCESSIBLE,
                true);

        SeatResponse expectedResponse = response(
                "A01",
                "A",
                SeatType.ACCESSIBLE,
                true);

        when(seatRepository.findById(SEAT_ID))
                .thenReturn(Optional.of(seat));

        when(seatRepository
                .existsByRoom_IdAndSeatNumberIgnoreCaseAndIdNot(
                        ROOM_ID,
                        "A01",
                        SEAT_ID))
                .thenReturn(false);

        when(seatMapper.toResponse(seat))
                .thenReturn(expectedResponse);

        SeatResponse result = seatService.update(
                SEAT_ID,
                request);

        assertThat(seat.getSeatNumber())
                .isEqualTo("A01");

        assertThat(seat.getRowLabel())
                .isEqualTo("A");

        assertThat(seat.getSeatType())
                .isEqualTo(SeatType.ACCESSIBLE);

        assertThat(seat.isActive())
                .isTrue();

        assertThat(result)
                .isSameAs(expectedResponse);

        verify(seatRepository)
                .findById(SEAT_ID);

        verify(seatRepository)
                .existsByRoom_IdAndSeatNumberIgnoreCaseAndIdNot(
                        ROOM_ID,
                        "A01",
                        SEAT_ID);

        verify(seatRepository, never())
                .save(any(Seat.class));

        verify(seatMapper)
                .toResponse(seat);
    }

    @Test
    void updateShouldThrowWhenSeatDoesNotExist() {
        UpdateSeatRequest request = new UpdateSeatRequest(
                "A01",
                "A",
                SeatType.STANDARD,
                true);

        when(seatRepository.findById(SEAT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> seatService.update(
                SEAT_ID,
                request))
                .isInstanceOf(NotFoundException.class);

        verify(seatRepository)
                .findById(SEAT_ID);

        verify(seatRepository, never())
                .existsByRoom_IdAndSeatNumberIgnoreCaseAndIdNot(
                        any(),
                        any(),
                        any());

        verifyNoInteractions(seatMapper);
    }

    @Test
    void updateShouldThrowWhenAnotherSeatHasSameNumber() {
        Room room = mock(Room.class);

        when(room.getId())
                .thenReturn(ROOM_ID);

        Seat seat = new Seat(
                room,
                "A01",
                "A",
                SeatType.STANDARD);

        UpdateSeatRequest request = new UpdateSeatRequest(
                "  B02  ",
                "  B  ",
                SeatType.VIP,
                true);

        when(seatRepository.findById(SEAT_ID))
                .thenReturn(Optional.of(seat));

        when(seatRepository
                .existsByRoom_IdAndSeatNumberIgnoreCaseAndIdNot(
                        ROOM_ID,
                        "B02",
                        SEAT_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> seatService.update(
                SEAT_ID,
                request))
                .isInstanceOf(ConflictException.class);

        assertThat(seat.getSeatNumber())
                .isEqualTo("A01");

        assertThat(seat.getRowLabel())
                .isEqualTo("A");

        assertThat(seat.getSeatType())
                .isEqualTo(SeatType.STANDARD);

        assertThat(seat.isActive())
                .isTrue();

        verify(seatRepository)
                .findById(SEAT_ID);

        verify(seatRepository)
                .existsByRoom_IdAndSeatNumberIgnoreCaseAndIdNot(
                        ROOM_ID,
                        "B02",
                        SEAT_ID);

        verify(seatRepository, never())
                .save(any(Seat.class));

        verify(seatMapper, never())
                .toResponse(any(Seat.class));
    }

    private Room activeRoom() {
        Cinema cinema = new Cinema(
                "CGV Vincom",
                "72 Le Thanh Ton",
                "Ho Chi Minh");

        return new Room(
                cinema,
                "Room 01",
                RoomType.STANDARD);
    }

    private Seat seat() {
        return new Seat(
                activeRoom(),
                "A01",
                "A",
                SeatType.STANDARD);
    }

    private SeatResponse response(
            String seatNumber,
            String rowLabel,
            SeatType seatType,
            boolean active) {

        return new SeatResponse(
                SEAT_ID,
                ROOM_ID,
                seatNumber,
                rowLabel,
                seatType,
                active,
                null,
                null);
    }
}
