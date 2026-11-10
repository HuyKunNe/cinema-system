package com.cinema.inventory.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.client.MovieCatalogClient;
import com.cinema.inventory.config.BookableShowtimeProperties;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.ShowtimeMapper;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.ShowtimeCreationService;
import com.cinema.inventory.service.ShowtimePricingService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ShowtimeManagementQueryTest {

    @Mock private ShowtimeRepository repository;
    @Mock private RoomRepository rooms;
    @Mock private CinemaRepository cinemas;
    @Mock private ShowtimeMapper mapper;
    @Mock private Clock clock;
    @Mock private BookableShowtimeProperties properties;
    @Mock private ShowtimeCreationService creationService;
    @Mock private MovieCatalogClient movieCatalog;
    @Mock private ShowtimePricingService pricingService;
    @InjectMocks private ShowtimeServiceImpl service;

    private final OffsetDateTime from = OffsetDateTime.parse("2026-11-10T00:00:00+07:00");
    private final OffsetDateTime to = from.plusDays(1);

    @Test
    void forwardsAllFiltersAndPreservesMappedPageMetadata() {
        UUID cinemaId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        var request = PageRequest.of(2, 10);
        var entity = mock(Showtime.class);
        var response = mock(ShowtimeResponse.class);
        when(repository.findManagedShowtimes(cinemaId, movieId, roomId,
                ShowtimeStatus.CANCELLED, from, to, request))
                .thenReturn(new PageImpl<>(List.of(entity), request, 21));
        when(mapper.toResponse(entity)).thenReturn(response);

        var result = service.getManagedShowtimes(cinemaId, movieId, roomId,
                ShowtimeStatus.CANCELLED, from, to, 2, 10);

        assertSame(response, result.content().getFirst());
        assertEquals(2, result.page().page());
        assertEquals(10, result.page().size());
        assertEquals(21, result.page().totalElements());
        assertEquals(3, result.page().totalPages());
        assertTrue(result.page().last());
        verify(repository).findManagedShowtimes(cinemaId, movieId, roomId,
                ShowtimeStatus.CANCELLED, from, to, request);
        verifyNoInteractions(rooms, cinemas, movieCatalog, creationService, pricingService);
    }

    @Test
    void noMatchingFiltersReturnsAnEmptyPage() {
        var request = PageRequest.of(0, 20);
        when(repository.findManagedShowtimes(null, null, null, null, from, to, request))
                .thenReturn(Page.empty(request));

        var result = service.getManagedShowtimes(null, null, null, null, from, to, 0, 20);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.page().totalElements());
        assertEquals(20, result.page().size());
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,-1", "0,101"})
    void rejectsInvalidPaginationBeforeQuerying(int page, int size) {
        var error = assertThrows(ValidationException.class,
                () -> service.getManagedShowtimes(null, null, null, null, from, to, page, size));

        assertEquals(InventoryErrorCode.INVALID_SHOWTIME_PAGINATION, error.getErrorCode());
        verifyNoInteractions(repository, mapper);
    }

    @Test
    void rejectsEmptyReversedAndMissingTimeBoundsBeforeQuerying() {
        assertThrows(ValidationException.class,
                () -> service.getManagedShowtimes(null, null, null, null, from, from, 0, 20));
        assertThrows(ValidationException.class,
                () -> service.getManagedShowtimes(null, null, null, null, to, from, 0, 20));
        assertThrows(ValidationException.class,
                () -> service.getManagedShowtimes(null, null, null, null, null, to, 0, 20));
        assertThrows(ValidationException.class,
                () -> service.getManagedShowtimes(null, null, null, null, from, null, 0, 20));
        verifyNoInteractions(repository, mapper);
    }
}
