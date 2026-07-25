package com.cinema.inventory.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.request.GenerateShowSeatsRequest;
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

  public ShowSeatServiceImpl(
      ShowSeatRepository showSeatRepository,
      ShowtimeRepository showtimeRepository,
      SeatRepository seatRepository,
      ShowSeatMapper showSeatMapper) {
    this.showSeatRepository = showSeatRepository;
    this.showtimeRepository = showtimeRepository;
    this.seatRepository = seatRepository;
    this.showSeatMapper = showSeatMapper;
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
}