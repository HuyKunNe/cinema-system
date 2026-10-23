package com.cinema.booking.mapper;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import com.cinema.booking.dto.response.BookingPricingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.api.pricing.BookingQuoteResponse;
import com.cinema.common.exception.exception.InternalServerException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BookingPricingSnapshotMapper {

    private static final long MAX_AMOUNT_VND = 99_999_999_999_999_999L;

    private final ObjectMapper objectMapper;

    public BookingPricingSnapshotMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Named("bookingPricing")
    public BookingPricingResponse toResponse(Booking booking) {
        String json = booking.getPricingQuoteSnapshotJson();

        if (json == null) {
            // PENDING/REJECTED policy bookings may have no final snapshot.
            if (booking.getPricingQuoteId() != null
                    && booking.getTotalAmount() != null) {
                throw invalid();
            }

            return null;
        }

        if (json.isBlank() || booking.getPricingQuoteId() == null) {
            throw invalid();
        }

        BookingQuoteResponse quote;

        try {
            quote = objectMapper.readValue(
                    json,
                    BookingQuoteResponse.class);
        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    BookingErrorCode.PRICING_SNAPSHOT_INVALID,
                    exception);
        }

        validate(booking, quote);

        var lines =
                quote.items().stream()
                        .map(line ->
                                new BookingPricingResponse.Line(
                                        line.showSeatId(),
                                        line.seatId(),
                                        line.seatNumber(),
                                        line.seatType(),
                                        line.capacity(),
                                        line.requestedTicketCategoryCode(),
                                        line.effectiveTicketCategoryCode(),
                                        line.effectiveTicketCategoryName(),
                                        line.categoryAdjustmentReason(),
                                        line.fareRuleId(),
                                        line.fareRuleCode(),
                                        line.fareUnit(),
                                        line.amountVnd()))
                        .toList();

        return new BookingPricingResponse(
                quote.pricingPolicyId(),
                quote.pricingPolicyVersion(),
                quote.timeZone(),
                quote.localStartsAt(),
                quote.experienceType(),
                quote.contentFormat(),
                quote.advanceScreening(),
                quote.currency(),
                quote.totalAmountVnd(),
                lines);
    }

    private void validate(
            Booking booking,
            BookingQuoteResponse quote) {

        if (quote == null
                || !booking.getPricingQuoteId().equals(quote.quoteId())
                || !booking.getShowtimeId().equals(quote.showtimeId())
                || quote.pricingPolicyId() == null
                || quote.pricingPolicyVersion() <= 0
                || quote.timeZone() == null
                || quote.localStartsAt() == null
                || quote.experienceType() == null
                || quote.contentFormat() == null
                || !"VND".equals(quote.currency())
                || !quote.currency().equals(booking.getCurrency())
                || booking.getTotalAmount() == null
                || quote.totalAmountVnd() == null
                || !quote.totalAmountVnd().matches("[0-9]{1,17}")
                || quote.items() == null
                || quote.items().isEmpty()
                || quote.items().size() > 100) {
            throw invalid();
        }

        long total = 0;
        Set<UUID> showSeatIds = new HashSet<>();
        Set<String> seatNumbers = new HashSet<>();

        for (var line : quote.items()) {
            if (line == null
                    || line.showSeatId() == null
                    || line.seatId() == null
                    || line.seatNumber() == null
                    || line.seatNumber().isBlank()
                    || line.seatType() == null
                    || line.requestedTicketCategoryCode() == null
                    || line.effectiveTicketCategoryCode() == null
                    || line.effectiveTicketCategoryName() == null
                    || line.fareRuleId() == null
                    || line.fareRuleCode() == null
                    || line.amountVnd() == null
                    || !line.amountVnd().matches("[0-9]{1,17}")
                    || !showSeatIds.add(line.showSeatId())
                    || !seatNumbers.add(line.seatNumber())) {
                throw invalid();
            }

            boolean couple = "COUPLE".equals(line.seatType());

            if (line.capacity() != (couple ? 2 : 1)
                    || !(couple ? "PER_PAIR" : "PER_TICKET")
                            .equals(line.fareUnit())) {
                throw invalid();
            }

            try {
                total = Math.addExact(
                        total,
                        Long.parseLong(line.amountVnd()));
            } catch (ArithmeticException exception) {
                throw invalid();
            }

            if (total > MAX_AMOUNT_VND) {
                throw invalid();
            }
        }

        if (total != Long.parseLong(quote.totalAmountVnd())
                || booking.getTotalAmount()
                        .compareTo(BigDecimal.valueOf(total)) != 0) {
            throw invalid();
        }
    }

    private static InternalServerException invalid() {
        return new InternalServerException(
                BookingErrorCode.PRICING_SNAPSHOT_INVALID);
    }
}
