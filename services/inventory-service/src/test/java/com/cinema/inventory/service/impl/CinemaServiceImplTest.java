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

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.request.CreateCinemaRequest;
import com.cinema.inventory.dto.request.UpdateCinemaRequest;
import com.cinema.inventory.dto.response.CinemaResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.mapper.CinemaMapper;
import com.cinema.inventory.repository.CinemaRepository;

@ExtendWith(MockitoExtension.class)
class CinemaServiceImplTest {

    private static final UUID CINEMA_ID = UUID.fromString(
            "019102b2-7c00-7000-8000-000000000001");

    @Mock
    private CinemaRepository cinemaRepository;

    @Mock
    private CinemaMapper cinemaMapper;

    private CinemaServiceImpl cinemaService;

    @BeforeEach
    void setUp() {
        cinemaService = new CinemaServiceImpl(
                cinemaRepository,
                cinemaMapper);
    }

    @Test
    void createShouldNormalizeSaveAndReturnResponse() {
        CreateCinemaRequest request = new CreateCinemaRequest(
                "  CGV Vincom  ",
                "  72 Le Thanh Ton  ",
                "  Ho Chi Minh  ");

        CinemaResponse expectedResponse = response(
                "CGV Vincom",
                "72 Le Thanh Ton",
                "Ho Chi Minh",
                true);

        when(cinemaRepository.save(any(Cinema.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(cinemaMapper.toResponse(any(Cinema.class)))
                .thenReturn(expectedResponse);

        CinemaResponse result = cinemaService.create(request);

        ArgumentCaptor<Cinema> cinemaCaptor = ArgumentCaptor.forClass(Cinema.class);

        verify(cinemaRepository)
                .save(cinemaCaptor.capture());

        Cinema savedCinema = cinemaCaptor.getValue();

        assertThat(savedCinema.getName())
                .isEqualTo("CGV Vincom");

        assertThat(savedCinema.getAddress())
                .isEqualTo("72 Le Thanh Ton");

        assertThat(savedCinema.getCity())
                .isEqualTo("Ho Chi Minh");

        assertThat(savedCinema.isActive())
                .isTrue();

        verify(cinemaMapper)
                .toResponse(savedCinema);

        assertThat(result)
                .isSameAs(expectedResponse);
    }

    @Test
    void getByIdShouldReturnMappedCinema() {
        Cinema cinema = cinema();
        CinemaResponse expectedResponse = response(
                cinema.getName(),
                cinema.getAddress(),
                cinema.getCity(),
                cinema.isActive());

        when(cinemaRepository.findById(CINEMA_ID))
                .thenReturn(Optional.of(cinema));

        when(cinemaMapper.toResponse(cinema))
                .thenReturn(expectedResponse);

        CinemaResponse result = cinemaService.getById(CINEMA_ID);

        assertThat(result)
                .isSameAs(expectedResponse);

        verify(cinemaRepository)
                .findById(CINEMA_ID);

        verify(cinemaMapper)
                .toResponse(cinema);
    }

    @Test
    void getByIdShouldThrowWhenCinemaDoesNotExist() {
        when(cinemaRepository.findById(CINEMA_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cinemaService.getById(CINEMA_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Cinema not found");

        verify(cinemaRepository)
                .findById(CINEMA_ID);

        verifyNoInteractions(cinemaMapper);
    }

    @Test
    void getActiveCinemasShouldFilterByNormalizedCity() {
        Cinema cinema = cinema();

        List<Cinema> cinemas = List.of(cinema);

        List<CinemaResponse> expectedResponses = List.of(response(
                cinema.getName(),
                cinema.getAddress(),
                cinema.getCity(),
                true));

        when(cinemaRepository
                .findAllByCityIgnoreCaseAndActiveTrueOrderByNameAsc(
                        "Ho Chi Minh"))
                .thenReturn(cinemas);

        when(cinemaMapper.toResponses(cinemas))
                .thenReturn(expectedResponses);

        List<CinemaResponse> result = cinemaService.getActiveCinemas(
                "  Ho Chi Minh  ");

        assertThat(result)
                .isSameAs(expectedResponses);

        verify(cinemaRepository)
                .findAllByCityIgnoreCaseAndActiveTrueOrderByNameAsc(
                        "Ho Chi Minh");

        verify(cinemaRepository, never())
                .findAllByActiveTrueOrderByNameAsc();
    }

    @Test
    void getActiveCinemasShouldReturnAllWhenCityIsNull() {
        Cinema cinema = cinema();

        List<Cinema> cinemas = List.of(cinema);

        List<CinemaResponse> expectedResponses = List.of(response(
                cinema.getName(),
                cinema.getAddress(),
                cinema.getCity(),
                true));

        when(cinemaRepository
                .findAllByActiveTrueOrderByNameAsc())
                .thenReturn(cinemas);

        when(cinemaMapper.toResponses(cinemas))
                .thenReturn(expectedResponses);

        List<CinemaResponse> result = cinemaService.getActiveCinemas(null);

        assertThat(result)
                .isSameAs(expectedResponses);

        verify(cinemaRepository)
                .findAllByActiveTrueOrderByNameAsc();

        verify(cinemaRepository, never())
                .findAllByCityIgnoreCaseAndActiveTrueOrderByNameAsc(
                        any());
    }

    @Test
    void getActiveCinemasShouldReturnAllWhenCityIsBlank() {
        when(cinemaRepository
                .findAllByActiveTrueOrderByNameAsc())
                .thenReturn(List.of());

        when(cinemaMapper.toResponses(List.of()))
                .thenReturn(List.of());

        List<CinemaResponse> result = cinemaService.getActiveCinemas("   ");

        assertThat(result)
                .isEmpty();

        verify(cinemaRepository)
                .findAllByActiveTrueOrderByNameAsc();

        verify(cinemaRepository, never())
                .findAllByCityIgnoreCaseAndActiveTrueOrderByNameAsc(
                        any());
    }

    @Test
    void updateShouldNormalizeAndUpdateCinema() {
        Cinema cinema = cinema();

        UpdateCinemaRequest request = new UpdateCinemaRequest(
                "  CGV Landmark 81  ",
                "  720A Dien Bien Phu  ",
                "  Ho Chi Minh  ",
                false);

        CinemaResponse expectedResponse = response(
                "CGV Landmark 81",
                "720A Dien Bien Phu",
                "Ho Chi Minh",
                false);

        when(cinemaRepository.findById(CINEMA_ID))
                .thenReturn(Optional.of(cinema));

        when(cinemaMapper.toResponse(cinema))
                .thenReturn(expectedResponse);

        CinemaResponse result = cinemaService.update(
                CINEMA_ID,
                request);

        assertThat(cinema.getName())
                .isEqualTo("CGV Landmark 81");

        assertThat(cinema.getAddress())
                .isEqualTo("720A Dien Bien Phu");

        assertThat(cinema.getCity())
                .isEqualTo("Ho Chi Minh");

        assertThat(cinema.isActive())
                .isFalse();

        assertThat(result)
                .isSameAs(expectedResponse);

        verify(cinemaRepository)
                .findById(CINEMA_ID);

        verify(cinemaRepository, never())
                .save(any(Cinema.class));

        verify(cinemaMapper)
                .toResponse(cinema);
    }

    @Test
    void updateShouldThrowWhenCinemaDoesNotExist() {
        UpdateCinemaRequest request = new UpdateCinemaRequest(
                "CGV Landmark 81",
                "720A Dien Bien Phu",
                "Ho Chi Minh",
                true);

        when(cinemaRepository.findById(CINEMA_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cinemaService.update(
                CINEMA_ID,
                request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Cinema not found");

        verify(cinemaRepository)
                .findById(CINEMA_ID);

        verifyNoInteractions(cinemaMapper);
    }

    private Cinema cinema() {
        return new Cinema(
                "CGV Vincom",
                "72 Le Thanh Ton",
                "Ho Chi Minh");
    }

    private CinemaResponse response(
            String name,
            String address,
            String city,
            boolean active) {

        return new CinemaResponse(
                CINEMA_ID,
                name,
                address,
                city,
                active,
                null,
                null);
    }
}
