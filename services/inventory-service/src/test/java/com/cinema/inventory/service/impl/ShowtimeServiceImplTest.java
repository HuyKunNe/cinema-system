package com.cinema.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.client.MovieCatalogClient;
import com.cinema.inventory.config.BookableShowtimeProperties;
import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.request.UpdateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.mapper.ShowtimeMapper;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.ShowtimeCreationService;
import com.cinema.inventory.service.ShowtimePricingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceImplTest {

    private static final UUID CINEMA_ID = UUID.fromString("019102b2-7c00-7000-8000-000000000001");

    private static final UUID ROOM_ID = UUID.fromString("019102b2-7c00-7000-8000-000000000002");

    private static final UUID MOVIE_ID = UUID.fromString("019102b2-7c00-7000-8000-000000000003");

    private static final UUID SHOWTIME_ID = UUID.fromString("019102b2-7c00-7000-8000-000000000004");

    private static final OffsetDateTime STARTS_AT =
            OffsetDateTime.parse("2026-08-01T10:00:00+07:00");

    private static final OffsetDateTime ENDS_AT = OffsetDateTime.parse("2026-08-01T12:00:00+07:00");

    @Mock private ShowtimeRepository showtimeRepository;

    @Mock private RoomRepository roomRepository;

    @Mock private CinemaRepository cinemaRepository;

    @Mock private ShowtimeMapper showtimeMapper;

    @Mock private ShowtimeCreationService showtimeCreationService;

    @Mock private MovieCatalogClient movieCatalogClient;

    @Mock private ShowtimePricingService showtimePricingService;

    private ShowtimeServiceImpl showtimeService;

    @BeforeEach
    void setUp() {
        showtimeService =
                new ShowtimeServiceImpl(
                        showtimeRepository,
                        roomRepository,
                        showtimeMapper,
                        Clock.fixed(STARTS_AT.minusDays(1).toInstant(), STARTS_AT.getOffset()),
                        new BookableShowtimeProperties(),
                        showtimeCreationService,
                        cinemaRepository,
                        movieCatalogClient,
                        showtimePricingService);
    }

    @Test
    void createShouldDelegateToCreationService() {
        CreateShowtimeRequest request = createRequest(STARTS_AT, ENDS_AT);
        ShowtimeResponse expected = response(STARTS_AT, ENDS_AT, ShowtimeStatus.SCHEDULED);

        when(showtimeCreationService.create(request)).thenReturn(expected);

        assertThat(showtimeService.create(request)).isSameAs(expected);

        verify(showtimeCreationService).create(request);
        verifyNoInteractions(showtimeRepository, roomRepository, cinemaRepository, showtimeMapper);
    }

    @Test
    void createShouldPropagateCreationConflict() {
        CreateShowtimeRequest request = createRequest(STARTS_AT, ENDS_AT);
        ConflictException failure =
                new ConflictException(
                        com.cinema.inventory.exception.InventoryErrorCode.SHOWTIME_OVERLAP);

        when(showtimeCreationService.create(request)).thenThrow(failure);

        assertThatThrownBy(() -> showtimeService.create(request)).isSameAs(failure);
    }

    @Test
    void getByIdShouldReturnMappedShowtime() {
        Showtime showtime = scheduledShowtime();

        ShowtimeResponse expectedResponse = response(STARTS_AT, ENDS_AT, ShowtimeStatus.SCHEDULED);

        when(showtimeRepository.findById(SHOWTIME_ID)).thenReturn(Optional.of(showtime));

        when(showtimeMapper.toResponse(showtime)).thenReturn(expectedResponse);

        ShowtimeResponse result = showtimeService.getById(SHOWTIME_ID);

        assertThat(result).isSameAs(expectedResponse);

        verify(showtimeRepository).findById(SHOWTIME_ID);

        verify(showtimeMapper).toResponse(showtime);
    }

    @Test
    void getByIdShouldThrowWhenShowtimeDoesNotExist() {
        when(showtimeRepository.findById(SHOWTIME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> showtimeService.getById(SHOWTIME_ID))
                .isInstanceOf(NotFoundException.class);

        verify(showtimeRepository).findById(SHOWTIME_ID);

        verifyNoInteractions(showtimeMapper);
    }

    @Test
    void getByRoomIdShouldReturnMappedShowtimes() {
        Room room = availableRoom();

        Showtime showtime = new Showtime(MOVIE_ID, room, STARTS_AT, ENDS_AT);

        List<Showtime> showtimes = List.of(showtime);

        List<ShowtimeResponse> expectedResponses =
                List.of(response(STARTS_AT, ENDS_AT, ShowtimeStatus.SCHEDULED));

        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

        when(showtimeRepository.findAllByRoom_IdOrderByStartsAtAsc(ROOM_ID)).thenReturn(showtimes);

        when(showtimeMapper.toResponses(showtimes)).thenReturn(expectedResponses);

        List<ShowtimeResponse> result = showtimeService.getByRoomId(ROOM_ID);

        assertThat(result).isSameAs(expectedResponses);

        verify(roomRepository).findById(ROOM_ID);

        verify(showtimeRepository).findAllByRoom_IdOrderByStartsAtAsc(ROOM_ID);

        verify(showtimeMapper).toResponses(showtimes);
    }

    @Test
    void getByRoomIdShouldThrowWhenRoomDoesNotExist() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> showtimeService.getByRoomId(ROOM_ID))
                .isInstanceOf(NotFoundException.class);

        verify(roomRepository).findById(ROOM_ID);

        verify(showtimeRepository, never()).findAllByRoom_IdOrderByStartsAtAsc(any());

        verifyNoInteractions(showtimeMapper);
    }

    @Test
    void getByMovieIdShouldReturnMappedShowtimes() {
        Showtime showtime = scheduledShowtime();

        List<Showtime> showtimes = List.of(showtime);

        List<ShowtimeResponse> expectedResponses =
                List.of(response(STARTS_AT, ENDS_AT, ShowtimeStatus.SCHEDULED));

        when(showtimeRepository.findAllByMovieIdOrderByStartsAtAsc(MOVIE_ID)).thenReturn(showtimes);

        when(showtimeMapper.toResponses(showtimes)).thenReturn(expectedResponses);

        List<ShowtimeResponse> result = showtimeService.getByMovieId(MOVIE_ID);

        assertThat(result).isSameAs(expectedResponses);

        verify(showtimeRepository).findAllByMovieIdOrderByStartsAtAsc(MOVIE_ID);

        verify(showtimeMapper).toResponses(showtimes);
    }

    @Test
    void getByTimeRangeShouldReturnMappedShowtimes() {
        Showtime showtime = scheduledShowtime();

        List<Showtime> showtimes = List.of(showtime);

        List<ShowtimeResponse> expectedResponses =
                List.of(response(STARTS_AT, ENDS_AT, ShowtimeStatus.SCHEDULED));

        when(showtimeRepository.findAllByStartsAtBetweenOrderByStartsAtAsc(STARTS_AT, ENDS_AT))
                .thenReturn(showtimes);

        when(showtimeMapper.toResponses(showtimes)).thenReturn(expectedResponses);

        List<ShowtimeResponse> result = showtimeService.getByTimeRange(STARTS_AT, ENDS_AT);

        assertThat(result).isSameAs(expectedResponses);

        verify(showtimeRepository).findAllByStartsAtBetweenOrderByStartsAtAsc(STARTS_AT, ENDS_AT);

        verify(showtimeMapper).toResponses(showtimes);
    }

    @Test
    void getByTimeRangeShouldThrowWhenRangeIsInvalid() {
        assertThatThrownBy(() -> showtimeService.getByTimeRange(STARTS_AT, STARTS_AT))
                .isInstanceOf(ValidationException.class);

        verify(showtimeRepository, never())
                .findAllByStartsAtBetweenOrderByStartsAtAsc(any(), any());

        verifyNoInteractions(showtimeMapper);
    }

    @Test
    void getByTimeRangeShouldThrowWhenStartIsNull() {
        assertThatThrownBy(() -> showtimeService.getByTimeRange(null, ENDS_AT))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(showtimeRepository, showtimeMapper);
    }

    @Test
    void updateShouldChangeScheduleAndReturnResponse() {
        Showtime showtime = scheduledShowtime();

        OffsetDateTime newStartsAt = STARTS_AT.plusDays(1);

        OffsetDateTime newEndsAt = ENDS_AT.plusDays(1);

        UpdateShowtimeRequest request =
                new UpdateShowtimeRequest(newStartsAt, newEndsAt, ShowtimeStatus.SCHEDULED);

        ShowtimeResponse expectedResponse =
                response(newStartsAt, newEndsAt, ShowtimeStatus.SCHEDULED);

        when(showtimeRepository.findById(SHOWTIME_ID)).thenReturn(Optional.of(showtime));

        when(showtimeRepository.existsOverlappingShowtimeExcludingId(
                        eq(ROOM_ID),
                        eq(SHOWTIME_ID),
                        eq(newStartsAt),
                        eq(newEndsAt),
                        anyCollection()))
                .thenReturn(false);

        when(showtimeMapper.toResponse(showtime)).thenReturn(expectedResponse);

        ShowtimeResponse result = showtimeService.update(SHOWTIME_ID, request);

        assertThat(showtime.getStartsAt()).isEqualTo(newStartsAt);

        assertThat(showtime.getEndsAt()).isEqualTo(newEndsAt);

        assertThat(showtime.getStatus()).isEqualTo(ShowtimeStatus.SCHEDULED);

        assertThat(result).isSameAs(expectedResponse);

        verify(showtimeRepository).findById(SHOWTIME_ID);

        verify(showtimeRepository)
                .existsOverlappingShowtimeExcludingId(
                        eq(ROOM_ID),
                        eq(SHOWTIME_ID),
                        eq(newStartsAt),
                        eq(newEndsAt),
                        anyCollection());

        verify(showtimeRepository, never()).save(any(Showtime.class));

        verify(showtimeMapper).toResponse(showtime);
    }

    @Test
    void updateShouldThrowWhenShowtimeDoesNotExist() {
        UpdateShowtimeRequest request =
                new UpdateShowtimeRequest(STARTS_AT, ENDS_AT, ShowtimeStatus.SCHEDULED);

        when(showtimeRepository.findById(SHOWTIME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> showtimeService.update(SHOWTIME_ID, request))
                .isInstanceOf(NotFoundException.class);

        verify(showtimeRepository).findById(SHOWTIME_ID);

        verify(showtimeRepository, never())
                .existsOverlappingShowtimeExcludingId(any(), any(), any(), any(), anyCollection());

        verifyNoInteractions(showtimeMapper);
    }

    @Test
    void updateShouldThrowWhenTimeRangeIsInvalid() {
        Showtime showtime = scheduledShowtime();

        UpdateShowtimeRequest request =
                new UpdateShowtimeRequest(STARTS_AT, STARTS_AT, ShowtimeStatus.SCHEDULED);

        when(showtimeRepository.findById(SHOWTIME_ID)).thenReturn(Optional.of(showtime));

        assertThatThrownBy(() -> showtimeService.update(SHOWTIME_ID, request))
                .isInstanceOf(ValidationException.class);

        verify(showtimeRepository, never())
                .existsOverlappingShowtimeExcludingId(any(), any(), any(), any(), anyCollection());

        verifyNoInteractions(showtimeMapper);
    }

    @Test
    void updateShouldThrowWhenShowtimeOverlaps() {
        Showtime showtime = scheduledShowtime();

        OffsetDateTime newStartsAt = STARTS_AT.plusHours(1);

        OffsetDateTime newEndsAt = ENDS_AT.plusHours(1);

        UpdateShowtimeRequest request =
                new UpdateShowtimeRequest(newStartsAt, newEndsAt, ShowtimeStatus.SCHEDULED);

        when(showtimeRepository.findById(SHOWTIME_ID)).thenReturn(Optional.of(showtime));

        when(showtimeRepository.existsOverlappingShowtimeExcludingId(
                        eq(ROOM_ID),
                        eq(SHOWTIME_ID),
                        eq(newStartsAt),
                        eq(newEndsAt),
                        anyCollection()))
                .thenReturn(true);

        assertThatThrownBy(() -> showtimeService.update(SHOWTIME_ID, request))
                .isInstanceOf(ConflictException.class);

        assertThat(showtime.getStartsAt()).isEqualTo(STARTS_AT);

        assertThat(showtime.getEndsAt()).isEqualTo(ENDS_AT);

        assertThat(showtime.getStatus()).isEqualTo(ShowtimeStatus.SCHEDULED);

        verify(showtimeRepository)
                .existsOverlappingShowtimeExcludingId(
                        eq(ROOM_ID),
                        eq(SHOWTIME_ID),
                        eq(newStartsAt),
                        eq(newEndsAt),
                        anyCollection());

        verify(showtimeRepository, never()).save(any(Showtime.class));

        verify(showtimeMapper, never()).toResponse(any(Showtime.class));
    }

    @Test
    void openForBookingShouldChangeStatusAndReturnResponse() {
        Showtime showtime = scheduledShowtime();
        Room room = showtime.getRoom();

        ShowtimeResponse expectedResponse =
                response(STARTS_AT, ENDS_AT, ShowtimeStatus.OPEN_FOR_BOOKING);

        when(showtimeRepository.findRoomIdForScheduleChange(SHOWTIME_ID))
                .thenReturn(Optional.of(ROOM_ID));

        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        when(showtimeRepository.findByIdForUpdate(SHOWTIME_ID)).thenReturn(Optional.of(showtime));

        when(showtimeMapper.toResponse(showtime)).thenReturn(expectedResponse);

        when(cinemaRepository.findByIdForUpdate(CINEMA_ID))
                .thenReturn(Optional.of(showtime.getRoom().getCinema()));

        ShowtimeResponse result = showtimeService.openForBooking(SHOWTIME_ID);

        assertThat(showtime.getStatus()).isEqualTo(ShowtimeStatus.OPEN_FOR_BOOKING);

        assertThat(result).isSameAs(expectedResponse);

        var order =
                org.mockito.Mockito.inOrder(showtimeRepository, roomRepository, cinemaRepository);

        order.verify(showtimeRepository).findRoomIdForScheduleChange(SHOWTIME_ID);

        order.verify(roomRepository).findByIdForUpdate(ROOM_ID);

        order.verify(cinemaRepository).findByIdForUpdate(CINEMA_ID);

        order.verify(showtimeRepository).findByIdForUpdate(SHOWTIME_ID);

        verify(showtimeMapper).toResponse(showtime);
    }

    @Test
    void openForBookingShouldThrowWhenRoomIsInactive() {
        Cinema cinema = mock(Cinema.class);
        Room room = mock(Room.class);

        when(cinema.getId()).thenReturn(CINEMA_ID);
        when(room.getCinema()).thenReturn(cinema);
        when(room.isActive()).thenReturn(false);

        when(room.isActive()).thenReturn(false);

        Showtime showtime = new Showtime(MOVIE_ID, room, STARTS_AT, ENDS_AT);

        when(showtimeRepository.findRoomIdForScheduleChange(SHOWTIME_ID))
                .thenReturn(Optional.of(ROOM_ID));

        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        when(showtimeRepository.findByIdForUpdate(SHOWTIME_ID)).thenReturn(Optional.of(showtime));

        when(cinemaRepository.findByIdForUpdate(CINEMA_ID))
                .thenReturn(Optional.of(showtime.getRoom().getCinema()));

        assertThatThrownBy(() -> showtimeService.openForBooking(SHOWTIME_ID))
                .isInstanceOf(ConflictException.class);

        assertThat(showtime.getStatus()).isEqualTo(ShowtimeStatus.SCHEDULED);

        verify(room).isActive();
        verify(showtimeRepository, never()).flush();
        verifyNoInteractions(showtimeMapper);
    }

    @Test
    void openForBookingShouldThrowWhenStatusIsNotScheduled() {
        Showtime showtime = scheduledShowtime();
        showtime.openForBooking();

        when(showtimeRepository.findRoomIdForScheduleChange(SHOWTIME_ID))
                .thenReturn(Optional.of(ROOM_ID));

        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(showtime.getRoom()));

        when(showtimeRepository.findByIdForUpdate(SHOWTIME_ID)).thenReturn(Optional.of(showtime));

        when(cinemaRepository.findByIdForUpdate(CINEMA_ID))
                .thenReturn(Optional.of(showtime.getRoom().getCinema()));

        assertThatThrownBy(() -> showtimeService.openForBooking(SHOWTIME_ID))
                .isInstanceOf(ConflictException.class);

        verify(showtimeRepository, never()).flush();
        verifyNoInteractions(showtimeMapper);
    }

    @Test
    void closeShouldChangeStatusAndReturnResponse() {
        Showtime showtime = scheduledShowtime();
        showtime.openForBooking();

        ShowtimeResponse expectedResponse = response(STARTS_AT, ENDS_AT, ShowtimeStatus.CLOSED);

        when(showtimeRepository.findById(SHOWTIME_ID)).thenReturn(Optional.of(showtime));

        when(showtimeMapper.toResponse(showtime)).thenReturn(expectedResponse);

        ShowtimeResponse result = showtimeService.close(SHOWTIME_ID);

        assertThat(showtime.getStatus()).isEqualTo(ShowtimeStatus.CLOSED);

        assertThat(result).isSameAs(expectedResponse);

        verify(showtimeRepository).findById(SHOWTIME_ID);

        verify(showtimeMapper).toResponse(showtime);
    }

    @Test
    void closeShouldThrowWhenStatusIsNotOpenForBooking() {
        Showtime showtime = scheduledShowtime();

        when(showtimeRepository.findById(SHOWTIME_ID)).thenReturn(Optional.of(showtime));

        assertThatThrownBy(() -> showtimeService.close(SHOWTIME_ID))
                .isInstanceOf(ConflictException.class);

        assertThat(showtime.getStatus()).isEqualTo(ShowtimeStatus.SCHEDULED);

        verify(showtimeRepository).findById(SHOWTIME_ID);

        verifyNoInteractions(showtimeMapper);
    }

    @Test
    void cancelShouldChangeStatusAndReturnResponse() {
        Showtime showtime = scheduledShowtime();

        ShowtimeResponse expectedResponse = response(STARTS_AT, ENDS_AT, ShowtimeStatus.CANCELLED);

        when(showtimeRepository.findById(SHOWTIME_ID)).thenReturn(Optional.of(showtime));

        when(showtimeMapper.toResponse(showtime)).thenReturn(expectedResponse);

        ShowtimeResponse result = showtimeService.cancel(SHOWTIME_ID);

        assertThat(showtime.getStatus()).isEqualTo(ShowtimeStatus.CANCELLED);

        assertThat(result).isSameAs(expectedResponse);

        verify(showtimeRepository).findById(SHOWTIME_ID);

        verify(showtimeMapper).toResponse(showtime);
    }

    @Test
    void cancelShouldThrowWhenShowtimeIsCompleted() {
        Showtime showtime = closedShowtime();
        showtime.complete();

        when(showtimeRepository.findById(SHOWTIME_ID)).thenReturn(Optional.of(showtime));

        assertThatThrownBy(() -> showtimeService.cancel(SHOWTIME_ID))
                .isInstanceOf(ConflictException.class);

        assertThat(showtime.getStatus()).isEqualTo(ShowtimeStatus.COMPLETED);

        verify(showtimeRepository).findById(SHOWTIME_ID);

        verifyNoInteractions(showtimeMapper);
    }

    @Test
    void completeShouldChangeStatusAndReturnResponse() {
        Showtime showtime = closedShowtime();

        ShowtimeResponse expectedResponse = response(STARTS_AT, ENDS_AT, ShowtimeStatus.COMPLETED);

        when(showtimeRepository.findById(SHOWTIME_ID)).thenReturn(Optional.of(showtime));

        when(showtimeMapper.toResponse(showtime)).thenReturn(expectedResponse);

        ShowtimeResponse result = showtimeService.complete(SHOWTIME_ID);

        assertThat(showtime.getStatus()).isEqualTo(ShowtimeStatus.COMPLETED);

        assertThat(result).isSameAs(expectedResponse);

        verify(showtimeRepository).findById(SHOWTIME_ID);

        verify(showtimeMapper).toResponse(showtime);
    }

    @Test
    void completeShouldThrowWhenShowtimeIsNotClosed() {
        Showtime showtime = scheduledShowtime();

        when(showtimeRepository.findById(SHOWTIME_ID)).thenReturn(Optional.of(showtime));

        assertThatThrownBy(() -> showtimeService.complete(SHOWTIME_ID))
                .isInstanceOf(ConflictException.class);

        assertThat(showtime.getStatus()).isEqualTo(ShowtimeStatus.SCHEDULED);

        verify(showtimeRepository).findById(SHOWTIME_ID);

        verifyNoInteractions(showtimeMapper);
    }

    @Test
    void stateChangeShouldThrowWhenShowtimeDoesNotExist() {
        when(showtimeRepository.findById(SHOWTIME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> showtimeService.cancel(SHOWTIME_ID))
                .isInstanceOf(NotFoundException.class);

        verify(showtimeRepository).findById(SHOWTIME_ID);

        verifyNoInteractions(showtimeMapper);
    }

    private CreateShowtimeRequest createRequest(OffsetDateTime startsAt, OffsetDateTime endsAt) {

        return new CreateShowtimeRequest(
                MOVIE_ID, ROOM_ID, startsAt, endsAt, null, UUID.randomUUID(), 15, null);
    }

    private Room availableRoom() {
        Cinema cinema = mock(Cinema.class);
        Room room = mock(Room.class);
        lenient().when(cinema.getId()).thenReturn(CINEMA_ID);

        lenient().when(room.getId()).thenReturn(ROOM_ID);

        lenient().when(room.isActive()).thenReturn(true);

        lenient().when(room.getCinema()).thenReturn(cinema);

        lenient().when(cinema.isActive()).thenReturn(true);

        return room;
    }

    private Showtime scheduledShowtime() {
        return new Showtime(MOVIE_ID, availableRoom(), STARTS_AT, ENDS_AT);
    }

    private Showtime closedShowtime() {
        Showtime showtime = scheduledShowtime();

        showtime.openForBooking();
        showtime.close();

        return showtime;
    }

    private ShowtimeResponse response(
            OffsetDateTime startsAt, OffsetDateTime endsAt, ShowtimeStatus status) {

        return new ShowtimeResponse(
                SHOWTIME_ID,
                MOVIE_ID,
                ROOM_ID,
                "Room 01",
                CINEMA_ID,
                "CGV Vincom",
                startsAt,
                endsAt,
                status,
                null,
                null);
    }
}
