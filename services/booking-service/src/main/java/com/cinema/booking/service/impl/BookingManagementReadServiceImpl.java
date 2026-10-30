package com.cinema.booking.service.impl;

import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.mapper.BookingMapper;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.service.BookingManagementReadService;
import com.cinema.common.api.mapper.PageResponseMapper;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.response.model.PageResponse;
import com.cinema.common.validation.UuidVersions;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class BookingManagementReadServiceImpl implements BookingManagementReadService {

    private final BookingRepository bookingRepository;

    private final BookingSeatRepository bookingSeatRepository;

    private final BookingMapper bookingMapper;

    public BookingManagementReadServiceImpl(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            BookingMapper bookingMapper) {

        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.bookingMapper = bookingMapper;
    }

    @Override
    public PageResponse<BookingResponse> findAll(
            BookingStatus status,
            UUID showtimeId,
            UUID userId,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo,
            int page,
            int size) {

        validateQuery(showtimeId, userId, createdFrom, createdTo, page, size);

        Specification<Booking> specification =
                (root, query, builder) -> {
                    List<Predicate> predicates = new ArrayList<>();

                    if (status != null) {
                        predicates.add(builder.equal(root.get("status"), status));
                    }

                    if (showtimeId != null) {
                        predicates.add(builder.equal(root.get("showtimeId"), showtimeId));
                    }

                    if (userId != null) {
                        predicates.add(builder.equal(root.get("userId"), userId));
                    }

                    if (createdFrom != null) {
                        predicates.add(
                                builder.greaterThanOrEqualTo(
                                        root.<OffsetDateTime>get("createdAt"), createdFrom));
                    }

                    if (createdTo != null) {
                        predicates.add(
                                builder.lessThan(root.<OffsetDateTime>get("createdAt"), createdTo));
                    }

                    return builder.and(predicates.toArray(Predicate[]::new));
                };

        PageRequest pageRequest =
                PageRequest.of(
                        page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<Booking> bookings = bookingRepository.findAll(specification, pageRequest);

        Map<UUID, List<BookingSeat>> seatsByBookingId = loadSeatsByBookingId(bookings.getContent());

        Page<BookingResponse> responses =
                bookings.map(
                        booking ->
                                bookingMapper.toResponse(
                                        booking,
                                        seatsByBookingId.getOrDefault(booking.getId(), List.of())));

        return PageResponseMapper.map(responses);
    }

    @Override
    public BookingResponse findById(UUID bookingId) {

        if (bookingId == null) {
            throw new ValidationException(BookingErrorCode.BOOKING_ID_REQUIRED);
        }

        if (bookingId.version() != 7) {
            throw invalidQuery();
        }

        Booking booking =
                bookingRepository
                        .findById(bookingId)
                        .orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(bookingId);

        return bookingMapper.toResponse(booking, seats);
    }

    private Map<UUID, List<BookingSeat>> loadSeatsByBookingId(List<Booking> bookings) {

        if (bookings.isEmpty()) {
            return Map.of();
        }

        List<UUID> bookingIds = bookings.stream().map(Booking::getId).toList();

        return bookingSeatRepository
                .findAllByBookingIdInOrderByBookingIdAscSeatNumberAsc(bookingIds)
                .stream()
                .collect(Collectors.groupingBy(BookingSeat::getBookingId));
    }

    private static void validateQuery(
            UUID showtimeId,
            UUID userId,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo,
            int page,
            int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw invalidQuery();
        }

        if (showtimeId != null && !UuidVersions.isV5OrV7(showtimeId)) {
            throw invalidQuery();
        }

        if (userId != null && userId.version() != 7) {
            throw invalidQuery();
        }

        if (createdFrom != null && createdTo != null && !createdFrom.isBefore(createdTo)) {

            throw invalidQuery();
        }
    }

    private static ValidationException invalidQuery() {

        return new ValidationException(BookingErrorCode.INVALID_MANAGEMENT_QUERY);
    }
}
