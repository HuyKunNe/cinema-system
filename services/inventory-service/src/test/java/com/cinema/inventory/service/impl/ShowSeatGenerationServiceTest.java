package com.cinema.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.inventory.dto.response.FareMatrixResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.entity.ShowtimeScreeningVersion;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.PricingDayGroup;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.service.FareMatrixService;
import com.cinema.inventory.service.ShowSeatGenerationService;
import com.cinema.inventory.service.ShowtimeLayoutService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ShowSeatGenerationServiceTest {

    private static final UUID CINEMA_ID = UUID.randomUUID();
    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final UUID SHOWTIME_ID = UUID.randomUUID();
    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID BAND_ID = UUID.randomUUID();
    private static final UUID GROUP_ID = UUID.randomUUID();

    @Mock private SeatRepository seatRepository;
    @Mock private ShowSeatRepository showSeatRepository;
    @Mock private ShowtimeLayoutService layoutService;
    @Mock private FareMatrixService fareMatrixService;

    private ShowSeatGenerationService generationService;

    @BeforeEach
    void setUp() {
        generationService =
                new ShowSeatGenerationService(
                        seatRepository,
                        showSeatRepository,
                        layoutService,
                        fareMatrixService);
    }

    @Test
    void publishedGenerationShouldPreserveZeroAndWholePairFares() {
        Showtime showtime = showtime();
        assignPublishedPricing(showtime);

        Seat standard = seat(showtime.getRoom(), "A1", SeatType.STANDARD);
        Seat couple = seat(showtime.getRoom(), "C1", SeatType.COUPLE);

        when(seatRepository.findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(ROOM_ID))
                .thenReturn(List.of(standard, couple));

        when(fareMatrixService.getForShowtimePricing(
                        CINEMA_ID, POLICY_ID, VERSION_ID))
                .thenReturn(matrix());

        when(showSeatRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<ShowSeat> result =
                generationService.generateFromPublishedPricing(showtime);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPrice()).isEqualByComparingTo("0");
        assertThat(result.get(1).getPrice()).isEqualByComparingTo("217000");
        assertThat(showtime.getShowSeats()).hasSize(2);
    }

    @Test
    void deferredValidationShouldNotGenerateSeatsOrPrices() {
        Showtime showtime = showtime();

        when(seatRepository.findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(ROOM_ID))
                .thenReturn(List.of(seat(showtime.getRoom(), "A1", SeatType.STANDARD)));

        generationService.validateForDeferredGeneration(showtime);

        assertThat(showtime.getShowSeats()).isEmpty();
        verifyNoInteractions(showSeatRepository, fareMatrixService);
    }

    private Showtime showtime() {
        Cinema cinema = new Cinema("Cinema", "Address", "City");
        ReflectionTestUtils.setField(cinema, "id", CINEMA_ID);

        Room room = new Room(cinema, "Room 01", RoomType.STANDARD);
        ReflectionTestUtils.setField(room, "id", ROOM_ID);

        Showtime showtime =
                new Showtime(
                        UUID.randomUUID(),
                        room,
                        OffsetDateTime.parse("2099-01-01T10:00:00Z"),
                        OffsetDateTime.parse("2099-01-01T12:00:00Z"));

        ReflectionTestUtils.setField(showtime, "id", SHOWTIME_ID);
        return showtime;
    }

    private Seat seat(Room room, String number, SeatType type) {
        Seat seat = new Seat(room, number, number.substring(0, 1), type);
        ReflectionTestUtils.setField(seat, "id", UUID.randomUUID());
        return seat;
    }

    private void assignPublishedPricing(Showtime showtime) {
        CinemaPricingPolicy policy = mock(CinemaPricingPolicy.class);
        CinemaPricingPolicyVersion version =
                mock(CinemaPricingPolicyVersion.class);
        ShowtimeScreeningVersion screening =
                mock(ShowtimeScreeningVersion.class);

        when(policy.getId()).thenReturn(POLICY_ID);
        when(version.getId()).thenReturn(VERSION_ID);
        when(version.getPolicy()).thenReturn(policy);
        when(version.getStatus()).thenReturn(PricingPolicyVersionStatus.PUBLISHED);
        when(version.appliesTo(CINEMA_ID)).thenReturn(true);
        when(screening.getContentFormat()).thenReturn(ContentFormat.TWO_D);

        showtime.replacePricingPolicyVersion(version);
        ReflectionTestUtils.setField(showtime, "screeningVersion", screening);
    }

    private FareMatrixResponse matrix() {
        return new FareMatrixResponse(
                POLICY_ID,
                VERSION_ID,
                1L,
                0L,
                PricingDayGroup.WEEKDAY,
                PricingPolicyVersionStatus.PUBLISHED,
                List.of(
                        new FareMatrixResponse.ScopeResponse(
                                PricingExperienceType.STANDARD,
                                ContentFormat.TWO_D)),
                List.of(
                        new FareMatrixResponse.TimeBandResponse(
                                BAND_ID, "Buổi tối", 1020, 1440, 0)),
                List.of(
                        new FareMatrixResponse.TicketGroupResponse(
                                GROUP_ID, "Mặc định", null, true, 0)),
                GROUP_ID,
                List.of(SeatType.STANDARD, SeatType.COUPLE),
                2,
                2,
                0,
                true,
                List.of(
                        cell(SeatType.STANDARD, BigDecimal.ZERO, FareUnit.SEAT),
                        cell(SeatType.COUPLE, new BigDecimal("217000"), FareUnit.PAIR)),
                new FareMatrixResponse.Options(
                        List.of(PricingExperienceType.STANDARD),
                        List.of(ContentFormat.TWO_D),
                        List.of(
                                new FareMatrixResponse.SeatTypeOption(
                                        SeatType.STANDARD, FareUnit.SEAT, 1),
                                new FareMatrixResponse.SeatTypeOption(
                                        SeatType.COUPLE, FareUnit.PAIR, 2)),
                        List.of(FareCellState.PRICED)));
    }

    private FareMatrixResponse.CellResponse cell(
            SeatType type, BigDecimal amount, FareUnit unit) {

        return new FareMatrixResponse.CellResponse(
                PricingExperienceType.STANDARD,
                ContentFormat.TWO_D,
                BAND_ID,
                GROUP_ID,
                type,
                FareCellState.PRICED,
                amount,
                unit);
    }
}
