package com.cinema.booking.event.validation;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


public final class BookingTicketSnapshotValidator {
    private BookingTicketSnapshotValidator() {}

    public static void validate(
            Booking booking, List<BookingSeat> seats, OutboxEventMessage event) {

        JsonNode payload = event.payload();

        if (booking.getTotalAmount() == null
                || booking.getConfirmedAt() == null
                || !payload.path("totalAmount").isNumber()
                || payload.path("totalAmount").decimalValue().compareTo(booking.getTotalAmount())
                        != 0
                || !Objects.equals(booking.getCurrency(), payload.path("currency").textValue())) {
            throw invalid();
        }

        try {
            String paymentId = payload.path("paymentId").textValue();

            if (paymentId == null || UUID.fromString(paymentId).version() != 7) throw invalid();

            String confirmed = payload.path("confirmedAt").textValue();

            if (confirmed == null) throw invalid();

            OffsetDateTime confirmedAt = OffsetDateTime.parse(confirmed);

            if (!confirmedAt.isEqual(booking.getConfirmedAt())
                    || !confirmedAt.isEqual(event.occurredAt())) {
                throw invalid();
            }

        } catch (IllegalArgumentException | DateTimeParseException failure) {
            throw invalid();
        }

        JsonNode lines = payload.path("seats");

        if (!lines.isArray() || lines.size() != seats.size() || seats.isEmpty()) throw invalid();

        Map<String, BookingSeat> expected = new HashMap<>();

        for (var seat : seats) {
            if (!seat.hasCompletedSnapshot() || expected.put(seat.getSeatNumber(), seat) != null)
                throw invalid();
        }

        BigDecimal total = BigDecimal.ZERO;

        for (var line : lines) {

            var seat = expected.remove(line.path("seatNumber").textValue());

            if (seat == null
                    || !Objects.equals(seat.getSeatType(), line.path("seatType").textValue())
                    || !line.path("price").isNumber()
                    || line.path("price").decimalValue().compareTo(seat.getPrice()) != 0)
                throw invalid();

            String group = line.path("ticketGroupId").textValue();

            if (!Objects.equals(
                    seat.getTicketGroupId() == null ? null : seat.getTicketGroupId().toString(),
                    group)) {
                throw invalid();
            }

            total = total.add(seat.getPrice());
        }

        if (!expected.isEmpty() || total.compareTo(booking.getTotalAmount()) != 0) throw invalid();
    }

    private static ValidationException invalid() {
        return new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
    }
}
