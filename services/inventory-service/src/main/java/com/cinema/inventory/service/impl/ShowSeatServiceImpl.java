package com.cinema.inventory.service.impl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.GenerateShowSeatsRequest;
import com.cinema.inventory.dto.request.HoldShowSeatRequest;
import com.cinema.inventory.dto.request.ShowSeatBookingRequest;
import com.cinema.inventory.dto.response.ShowSeatResponse;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.ShowSeatMapper;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.ShowSeatService;

@Service
public class ShowSeatServiceImpl implements ShowSeatService {

    private final ShowSeatRepository showSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final ShowSeatMapper showSeatMapper;
    private final Clock clock;

    public ShowSeatServiceImpl(
            ShowSeatRepository showSeatRepository,
            ShowtimeRepository showtimeRepository,
            SeatRepository seatRepository,
            ShowSeatMapper showSeatMapper,
            Clock clock) {

        this.showSeatRepository = showSeatRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.showSeatMapper = showSeatMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<ShowSeatResponse> generate(
            UUID showtimeId,
            GenerateShowSeatsRequest request) {

        Showtime showtime = findShowtime(showtimeId);

        validateShowtimeEditable(showtime);
        validateShowSeatsNotGenerated(showtimeId);

        List<Seat> activeSeats = seatRepository
                .findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(
                        showtime.getRoom().getId());

        if (activeSeats.isEmpty()) {
            throw new ConflictException(InventoryErrorCode.NO_ACTIVE_SEATS);
        }

        List<ShowSeat> showSeats = activeSeats.stream()
                .map(seat -> new ShowSeat(
                        showtime,
                        seat,
                        request.defaultPrice()))
                .toList();

        List<ShowSeat> savedShowSeats = showSeatRepository.saveAll(showSeats);

        return showSeatMapper.toResponses(savedShowSeats);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowSeatResponse getById(UUID showSeatId) {
        return showSeatMapper.toResponse(findShowSeat(showSeatId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowSeatResponse> getByShowtimeId(UUID showtimeId) {

        findShowtime(showtimeId);

        List<ShowSeat> showSeats = showSeatRepository
                .findAllByShowtime_IdOrderBySeatNumberAsc(showtimeId);

        return showSeatMapper.toResponses(showSeats);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowSeatResponse> getAvailableByShowtimeId(UUID showtimeId) {

        findShowtime(showtimeId);

        List<ShowSeat> showSeats = showSeatRepository
                .findAllByShowtime_IdAndStatusOrderBySeatNumberAsc(
                        showtimeId,
                        ShowSeatStatus.AVAILABLE);

        return showSeatMapper.toResponses(showSeats);
    }

    private Showtime findShowtime(UUID showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));
    }

    private ShowSeat findShowSeat(UUID showSeatId) {
        return showSeatRepository.findById(showSeatId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.SHOW_SEAT_NOT_FOUND));
    }

    private void validateShowtimeEditable(Showtime showtime) {

        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_EDITABLE);
        }
    }

    private void validateShowSeatsNotGenerated(UUID showtimeId) {

        if (showSeatRepository.existsByShowtime_Id(showtimeId)) {
            throw new ConflictException(InventoryErrorCode.SHOW_SEATS_ALREADY_GENERATED);
        }
    }

    @Override
    @Transactional
    public ShowSeatResponse hold(UUID showSeatId, HoldShowSeatRequest request) {
        ShowSeat showSeat = findShowSeatForUpdate(showSeatId);

        OffsetDateTime now = OffsetDateTime.now(clock);

        if (!request.expiresAt().isAfter(now)) {
            throw new ValidationException(InventoryErrorCode.INVALID_HOLD_EXPIRATION);
        }

        if (showSeat.getStatus() != ShowSeatStatus.AVAILABLE) {

            throw new ConflictException(InventoryErrorCode.SHOW_SEAT_NOT_AVAILABLE);
        }

        showSeat.hold(
                request.bookingId(),
                request.expiresAt(),
                now);

        return showSeatMapper.toResponse(showSeat);
    }

    @Override
    @Transactional
    public ShowSeatResponse book(UUID showSeatId, ShowSeatBookingRequest request) {
        ShowSeat showSeat = findShowSeatForUpdate(showSeatId);

        validateHeldBy(showSeat, request.bookingId());

        OffsetDateTime now = OffsetDateTime.now(clock);

        if (showSeat.hasExpired(now)) {
            throw new ConflictException(InventoryErrorCode.SHOW_SEAT_HOLD_EXPIRED);
        }

        showSeat.book(request.bookingId());

        return showSeatMapper.toResponse(showSeat);
    }

    @Override
    public ShowSeatResponse release(UUID showSeatId, ShowSeatBookingRequest request) {
        ShowSeat showSeat = findShowSeatForUpdate(showSeatId);

        validateHeldBy(showSeat, request.bookingId());

        showSeat.release(request.bookingId());

        return showSeatMapper.toResponse(showSeat);
    }

    private ShowSeat findShowSeatForUpdate(UUID showSeatId) {

        return showSeatRepository
                .findByIdForUpdate(showSeatId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.SHOW_SEAT_NOT_FOUND));
    }

    private void validateHeldBy(
            ShowSeat showSeat,
            UUID bookingId) {

        if (showSeat.getStatus() != ShowSeatStatus.HELD) {

            throw new ConflictException(InventoryErrorCode.SHOW_SEAT_NOT_HELD);
        }

        if (!showSeat.isHeldBy(bookingId)) {
            throw new ConflictException(InventoryErrorCode.SHOW_SEAT_HELD_BY_ANOTHER_BOOKING);
        }
    }
}
