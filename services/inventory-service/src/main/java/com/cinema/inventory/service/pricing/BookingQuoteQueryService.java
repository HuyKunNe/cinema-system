package com.cinema.inventory.service.pricing;

import com.cinema.common.api.pricing.BookingQuoteResponse;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.entity.PricingQuote;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.PricingQuoteRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class BookingQuoteQueryService {

    private final PricingQuoteRepository quoteRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public BookingQuoteQueryService(
            PricingQuoteRepository quoteRepository, ObjectMapper objectMapper, Clock clock) {

        this.quoteRepository = quoteRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public BookingQuoteResponse get(UUID userId, UUID quoteId) {
        if (userId == null || quoteId == null) {
            throw notFound();
        }

        PricingQuote quote =
                quoteRepository
                        .findByIdAndUserId(quoteId, userId)
                        .orElseThrow(BookingQuoteQueryService::notFound);

        if (!quote.getExpiresAt().isAfter(OffsetDateTime.now(clock))) {
            throw new ConflictException(InventoryErrorCode.QUOTE_EXPIRED);
        }
        return readStoredSnapshot(quote);
    }

    BookingQuoteResponse readStoredSnapshot(PricingQuote quote) {
        BookingQuoteResponse snapshot = readSnapshot(quote);
        validateHeader(quote, snapshot);
        return snapshot;
    }

    private BookingQuoteResponse readSnapshot(PricingQuote quote) {
        String json = quote.getSnapshotJson();

        if (json == null || json.isBlank()) {
            throw invalidSnapshot();
        }

        try {
            return objectMapper.readValue(json, BookingQuoteResponse.class);
        } catch (JsonProcessingException exception) {
            throw new InternalServerException(InventoryErrorCode.QUOTE_SNAPSHOT_INVALID, exception);
        }
    }

    private void validateHeader(PricingQuote quote, BookingQuoteResponse snapshot) {

        if (snapshot == null
                || !quote.getId().equals(snapshot.quoteId())
                || !quote.getShowtimeId().equals(snapshot.showtimeId())
                || !Objects.equals(quote.getShowtimeVersion(), snapshot.showtimeVersion())
                || !quote.getPolicyId().equals(snapshot.pricingPolicyId())
                || quote.getPolicyVersion() != snapshot.pricingPolicyVersion()
                || snapshot.issuedAt() == null
                || snapshot.expiresAt() == null
                || !quote.getIssuedAt().toInstant().equals(snapshot.issuedAt().toInstant())
                || !quote.getExpiresAt().toInstant().equals(snapshot.expiresAt().toInstant())
                || snapshot.items() == null
                || snapshot.items().isEmpty()
                || snapshot.items().stream().anyMatch(Objects::isNull)
                || snapshot.totalAmountVnd() == null
                || !snapshot.totalAmountVnd().matches("[0-9]{1,17}")
                || !"VND".equals(snapshot.currency())
                || snapshot.seatsHeld()) {

            throw invalidSnapshot();
        }
    }

    private static NotFoundException notFound() {
        return new NotFoundException(InventoryErrorCode.QUOTE_NOT_FOUND);
    }

    private static InternalServerException invalidSnapshot() {
        return new InternalServerException(InventoryErrorCode.QUOTE_SNAPSHOT_INVALID);
    }
}
