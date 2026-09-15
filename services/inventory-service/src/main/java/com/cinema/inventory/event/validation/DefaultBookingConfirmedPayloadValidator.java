package com.cinema.inventory.event.validation;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.BookingConfirmedPayload;
import com.cinema.inventory.event.payload.ConfirmedSeatPayload;
import com.cinema.inventory.exception.InventoryErrorCode;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class DefaultBookingConfirmedPayloadValidator implements BookingConfirmedPayloadValidator {

    private static final int MAX_SEAT_NUMBER_LENGTH = 20;

    private static final int MAX_PRICE_PRECISION = 12;

    private static final int MAX_TOTAL_PRECISION = 19;

    private static final int MAX_MONEY_SCALE = 2;

    @Override
    public void validate(
            String partitionKey, OutboxEventMessage message, BookingConfirmedPayload payload) {

        if (message == null || payload == null) {
            throw invalidPayload();
        }

        validateIdentifiers(partitionKey, message, payload);

        validateSeats(payload);

        validateMoney(payload.totalAmount(), MAX_TOTAL_PRECISION);

        validateSeatTotal(payload);

        validateCurrency(payload.currency());

        validateConfirmedAt(message.occurredAt(), payload.confirmedAt());
    }

    private static void validateIdentifiers(
            String partitionKey, OutboxEventMessage message, BookingConfirmedPayload payload) {

        requireUuidV7(payload.bookingId());

        requireUuidV7(payload.userId());

        requireUuidV7(payload.showtimeId());

        requireUuidV7(payload.paymentId());

        if (!payload.bookingId().equals(message.aggregateId())) {
            throw invalidPayload();
        }

        if (partitionKey == null || !payload.bookingId().toString().equals(partitionKey)) {

            throw new ValidationException(InventoryErrorCode.EVENT_PARTITION_KEY_INVALID);
        }
    }

    private static void validateSeats(BookingConfirmedPayload payload) {

        if (payload.seats() == null || payload.seats().isEmpty()) {
            throw invalidPayload();
        }

        Set<String> seatNumbers = new HashSet<>();

        for (ConfirmedSeatPayload seat : payload.seats()) {

            validateSeat(seat);

            if (!seatNumbers.add(seat.seatNumber())) {
                throw invalidPayload();
            }
        }
    }

    private static void validateSeat(ConfirmedSeatPayload seat) {

        if (seat == null) {
            throw invalidPayload();
        }

        validateSeatNumber(seat.seatNumber());

        validateSeatType(seat.seatType());

        validateMoney(seat.price(), MAX_PRICE_PRECISION);
    }

    private static void validateSeatNumber(String seatNumber) {

        if (seatNumber == null
                || seatNumber.isBlank()
                || seatNumber.length() > MAX_SEAT_NUMBER_LENGTH) {

            throw invalidPayload();
        }

        String canonical = seatNumber.strip().toUpperCase(Locale.ROOT);

        if (!canonical.equals(seatNumber)) {
            throw invalidPayload();
        }
    }

    private static void validateSeatType(String seatType) {

        if (seatType == null
                || seatType.isBlank()
                || !seatType.equals(seatType.strip())
                || !seatType.equals(seatType.toUpperCase(Locale.ROOT))) {

            throw invalidPayload();
        }

        boolean supported =
                Arrays.stream(SeatType.values()).map(Enum::name).anyMatch(seatType::equals);

        if (!supported) {
            throw invalidPayload();
        }
    }

    private static void validateMoney(BigDecimal value, int maximumPrecision) {

        if (value == null || value.signum() < 0) {
            throw invalidPayload();
        }

        BigDecimal normalized = value.stripTrailingZeros();

        int normalizedScale = Math.max(normalized.scale(), 0);

        int integerDigits = Math.max(normalized.precision() - normalized.scale(), 0);

        if (normalizedScale > MAX_MONEY_SCALE
                || integerDigits > maximumPrecision - MAX_MONEY_SCALE) {

            throw invalidPayload();
        }
    }

    private static void validateSeatTotal(BookingConfirmedPayload payload) {

        BigDecimal calculatedTotal =
                payload.seats().stream()
                        .map(ConfirmedSeatPayload::price)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (calculatedTotal.compareTo(payload.totalAmount()) != 0) {
            throw invalidPayload();
        }
    }

    private static void validateCurrency(String currency) {

        if (!InventoryEventContract.CURRENCY_VND.equals(currency)) {
            throw invalidPayload();
        }
    }

    private static void validateConfirmedAt(OffsetDateTime occurredAt, OffsetDateTime confirmedAt) {

        if (occurredAt == null || confirmedAt == null || !confirmedAt.isEqual(occurredAt)) {

            throw invalidPayload();
        }
    }

    private static void requireUuidV7(UUID value) {

        if (value == null || value.version() != 7) {
            throw invalidPayload();
        }
    }

    private static ValidationException invalidPayload() {

        return new ValidationException(InventoryErrorCode.EVENT_PAYLOAD_INVALID);
    }
}
