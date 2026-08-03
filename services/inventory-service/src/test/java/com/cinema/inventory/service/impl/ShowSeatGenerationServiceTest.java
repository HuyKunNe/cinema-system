package com.cinema.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.service.SeatPricingPolicy;
import com.cinema.inventory.service.ShowSeatGenerationService;

@ExtendWith(MockitoExtension.class)
class ShowSeatGenerationServiceTest {

    private static final UUID ROOM_ID = UUID.randomUUID();

    private static final UUID SHOWTIME_ID = UUID.randomUUID();

    private static final BigDecimal BASE_PRICE = new BigDecimal("100000.00");

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ShowSeatRepository showSeatRepository;

    @Mock
    private SeatPricingPolicy seatPricingPolicy;

    private ShowSeatGenerationService generationService;

    @BeforeEach
    void setUp() {
        generationService = new ShowSeatGenerationService(
                seatRepository,
                showSeatRepository,
                seatPricingPolicy);
    }

    @Test
    void generateShouldCreateShowSeatsForActiveSeats() {
        Showtime showtime = showtime();
        Seat standardSeat = seat("A1", SeatType.STANDARD);

        Seat coupleSeat = seat("C1", SeatType.COUPLE);

        when(seatRepository
                .findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(
                        ROOM_ID))
                .thenReturn(List.of(
                        standardSeat,
                        coupleSeat));

        when(showSeatRepository
                .findAllByShowtime_IdOrderBySeatNumberAsc(
                        SHOWTIME_ID))
                .thenReturn(List.of());

        when(seatPricingPolicy.calculate(
                BASE_PRICE,
                SeatType.STANDARD))
                .thenReturn(
                        new BigDecimal("100000.00"));

        when(seatPricingPolicy.calculate(
                BASE_PRICE,
                SeatType.COUPLE))
                .thenReturn(
                        new BigDecimal("200000.00"));

        when(showSeatRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<ShowSeat> result = generationService.generate(
                showtime,
                BASE_PRICE);

        assertThat(result)
                .hasSize(2);

        assertThat(result)
                .extracting(ShowSeat::getSeatNumber)
                .containsExactly("A1", "C1");

        assertThat(result.get(0).getPrice())
                .isEqualByComparingTo("100000.00");

        assertThat(result.get(1).getPrice())
                .isEqualByComparingTo("200000.00");

        assertThat(showtime.getShowSeats())
                .hasSize(2);

        verify(showSeatRepository)
                .saveAll(anyList());
    }

    @Test
    void generateShouldSkipExistingShowSeat() {
        Showtime showtime = showtime();
        Seat seat = seat("A1", SeatType.STANDARD);

        ShowSeat existing = new ShowSeat(
                showtime,
                seat,
                new BigDecimal("100000.00"));

        when(seatRepository
                .findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(
                        ROOM_ID))
                .thenReturn(List.of(seat));

        when(showSeatRepository
                .findAllByShowtime_IdOrderBySeatNumberAsc(
                        SHOWTIME_ID))
                .thenReturn(List.of(existing));

        List<ShowSeat> result = generationService.generate(
                showtime,
                BASE_PRICE);

        assertThat(result).isEmpty();

        verify(showSeatRepository, never())
                .saveAll(anyList());

        verifyNoInteractions(seatPricingPolicy);
    }

    @Test
    void generateShouldThrowWhenRoomHasNoActiveSeats() {
        Showtime showtime = showtime();

        when(seatRepository
                .findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(
                        ROOM_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> generationService.generate(
                showtime,
                BASE_PRICE))
                .isInstanceOf(ConflictException.class);

        verify(showSeatRepository, never())
                .saveAll(anyList());

        verifyNoInteractions(seatPricingPolicy);
    }

    private Showtime showtime() {
        Cinema cinema = new Cinema(
                "CGV Vincom",
                "72 Le Thanh Ton",
                "Ho Chi Minh");

        Room room = new Room(
                cinema,
                "Room 01",
                RoomType.STANDARD);

        setId(room, ROOM_ID);

        Showtime showtime = new Showtime(
                UUID.randomUUID(),
                room,
                OffsetDateTime.parse(
                        "2099-01-01T03:00:00Z"),
                OffsetDateTime.parse(
                        "2099-01-01T05:00:00Z"));

        setId(showtime, SHOWTIME_ID);

        return showtime;
    }

    private Seat seat(
            String seatNumber,
            SeatType seatType) {

        Room room = showtime().getRoom();

        Seat seat = new Seat(
                room,
                seatNumber,
                seatNumber.substring(0, 1),
                seatType);

        setId(seat, UUID.randomUUID());

        return seat;
    }

    private void setId(
            Object entity,
            UUID id) {

        /*
         * Dùng helper thiết lập ID hiện có của common-test
         * hoặc ReflectionTestUtils nếu BaseEntity không có setter.
         */
        org.springframework.test.util.ReflectionTestUtils
                .setField(entity, "id", id);
    }
}
