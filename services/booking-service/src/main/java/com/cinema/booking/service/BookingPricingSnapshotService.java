package com.cinema.booking.service;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.event.payload.ReservedSeatPayload;
import com.cinema.booking.event.payload.SeatReservedPayload;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.api.pricing.BookingQuoteResponse;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class BookingPricingSnapshotService {

    private static final long MAX_AMOUNT_VND = 99_999_999_999_999_999L;

    private final ObjectMapper objectMapper;

    public BookingPricingSnapshotService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void apply(
            Booking booking, List<BookingSeat> bookingSeats, SeatReservedPayload payload) {

        BookingQuoteResponse quote = payload.pricingQuote();

        validateHeader(booking, payload, quote);

        Map<UUID, ReservedSeatPayload> reservedById = new HashMap<>();

        for (ReservedSeatPayload seat : payload.seats()) {
            if (reservedById.putIfAbsent(seat.inventorySeatId(), seat) != null) {
                throw invalid();
            }
        }

        Map<String, BookingQuoteResponse.Line> linesByNumber = new HashMap<>();

        Set<UUID> showSeatIds = new HashSet<>();
        long total = 0;

        for (var line : quote.items()) {
            if (line == null
                    || line.showSeatId() == null
                    || line.seatId() == null
                    || line.seatNumber() == null
                    || line.seatNumber().isBlank()
                    || line.fareRuleId() == null
                    || line.fareRuleCode() == null
                    || line.fareRuleCode().isBlank()
                    || line.requestedTicketCategoryCode() == null
                    || line.requestedTicketCategoryCode().isBlank()
                    || line.effectiveTicketCategoryCode() == null
                    || line.effectiveTicketCategoryCode().isBlank()
                    || line.amountVnd() == null
                    || !line.amountVnd().matches("[0-9]{1,17}")
                    || !showSeatIds.add(line.showSeatId())) {
                throw invalid();
            }

            if (linesByNumber.putIfAbsent(normalize(line.seatNumber()), line) != null) {
                throw invalid();
            }

            ReservedSeatPayload reserved = reservedById.get(line.showSeatId());

            if (reserved == null
                    || !normalize(reserved.seatNumber()).equals(normalize(line.seatNumber()))
                    || !reserved.seatType().equals(line.seatType())) {
                throw mismatch();
            }

            boolean couple = "COUPLE".equals(line.seatType());

            if (line.capacity() != (couple ? 2 : 1)
                    || !(couple ? "PER_PAIR" : "PER_TICKET").equals(line.fareUnit())) {
                throw invalid();
            }

            long amount = Long.parseLong(line.amountVnd());

            if (reserved.price().compareTo(BigDecimal.valueOf(amount)) != 0) {
                throw mismatch();
            }

            if (line.eligibilityEvidence() == null) {
                throw invalid();
            }

            for (var evidence : line.eligibilityEvidence()) {
                if (evidence == null
                        || evidence.ruleCode() == null
                        || evidence.ruleCode().isBlank()
                        || evidence.reference() == null
                        || evidence.reference().isBlank()
                        || evidence.checkedAt() == null
                        || evidence.checkedAt().isAfter(payload.heldAt())
                        || (evidence.validUntil() != null
                                && evidence.validUntil().isBefore(payload.holdExpiresAt()))) {
                    throw invalid();
                }
            }

            try {
                total = Math.addExact(total, amount);
            } catch (ArithmeticException exception) {
                throw invalid();
            }

            if (total > MAX_AMOUNT_VND) {
                throw invalid();
            }
        }

        if (!showSeatIds.equals(reservedById.keySet())
                || linesByNumber.size() != bookingSeats.size()
                || total != Long.parseLong(quote.totalAmountVnd())
                || payload.totalAmount().compareTo(BigDecimal.valueOf(total)) != 0) {
            throw mismatch();
        }

        for (BookingSeat seat : bookingSeats) {
            var line = linesByNumber.get(normalize(seat.getSeatNumber()));

            if (line == null
                    || !line.showSeatId().equals(seat.getInventorySeatId())
                    || !line.seatType().equals(seat.getSeatType())
                    || seat.getPrice().compareTo(new BigDecimal(line.amountVnd())) != 0) {
                throw mismatch();
            }
        }

        // Validate everything before completing pricing snapshots.
        String quoteJson = serialize(quote);

        Map<String, String> lineJsonByNumber = new HashMap<>();

        for (var entry : linesByNumber.entrySet()) {
            lineJsonByNumber.put(entry.getKey(), serialize(entry.getValue()));
        }

        booking.completePricingQuoteSnapshot(
                quote.quoteId(), quoteJson, payload.heldAt(), payload.holdExpiresAt());

        for (BookingSeat seat : bookingSeats) {
            seat.completePricingSnapshot(lineJsonByNumber.get(normalize(seat.getSeatNumber())));
        }
    }

    private void validateHeader(
            Booking booking, SeatReservedPayload payload, BookingQuoteResponse quote) {

        if (quote == null
                || quote.quoteId() == null
                || booking.getPricingQuoteId() == null
                || !booking.getPricingQuoteId().equals(quote.quoteId())
                || !booking.getShowtimeId().equals(quote.showtimeId())
                || quote.pricingPolicyId() == null
                || quote.pricingPolicyVersion() <= 0
                || quote.showtimeVersion() == null
                || quote.showtimeVersion() < 0
                || quote.timeZone() == null
                || quote.experienceType() == null
                || quote.contentFormat() == null
                || quote.officialReleaseDate() == null
                || quote.localStartsAt() == null
                || quote.items() == null
                || quote.items().isEmpty()
                || quote.items().size() > 100
                || quote.totalAmountVnd() == null
                || !quote.totalAmountVnd().matches("[0-9]{1,17}")
                || !"VND".equals(quote.currency())
                || !quote.currency().equals(payload.currency())
                || quote.issuedAt() == null
                || quote.expiresAt() == null
                || !quote.expiresAt().isAfter(quote.issuedAt())
                || payload.heldAt().isBefore(quote.issuedAt())
                || !payload.heldAt().isBefore(quote.expiresAt())
                || payload.holdExpiresAt().isAfter(quote.expiresAt())
                || payload.holdExpiresAt().isAfter(quote.localStartsAt())
                || payload.holdExpiresAt().isAfter(booking.getExpiresAt())
                || quote.seatsHeld()) {
            throw mismatch();
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            // Infrastructure failure: let the transaction roll back.
            throw new IllegalStateException(
                    "Failed to serialize booking pricing snapshot", exception);
        }
    }

    private static String normalize(String seatNumber) {
        return seatNumber.trim().toUpperCase(Locale.ROOT);
    }

    private static ValidationException invalid() {
        return new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
    }

    private static ConflictException mismatch() {
        return new ConflictException(BookingErrorCode.RESERVATION_RESULT_MISMATCH);
    }
}
