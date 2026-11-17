package com.cinema.booking.service;

import com.cinema.booking.repository.BookingRepository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ShowtimeEndBackfillService {
    private static final long BATCH_BUDGET_NANOS = Duration.ofSeconds(3).toNanos();
    private final BookingRepository repository;
    private final TransactionTemplate transaction;
    private final RestClient inventory;
    private final Clock clock;

    public ShowtimeEndBackfillService(
            BookingRepository repository,
            PlatformTransactionManager manager,
            @Qualifier("systemClock") Clock clock,
            @Value("${cinema.booking.inventory-base-url:http://localhost:8083}") String baseUrl) {
        this.repository = repository;
        this.transaction = new TransactionTemplate(manager);
        this.clock = clock;
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(1))
                                .followRedirects(HttpClient.Redirect.NEVER)
                                .build());
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.inventory = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public Batch run(UUID afterId, int size, boolean dryRun, UUID actor) {
        long startedAt = System.nanoTime();
        // Repository transaction ends before HTTP calls; no Booking lock spans network IO.
        var candidates = repository.findMissingShowtimeEnds(afterId, PageRequest.of(0, size + 1));
        boolean hasMore = candidates.size() > size;
        var selected = candidates.subList(0, Math.min(size, candidates.size()));
        var rows = new ArrayList<Row>();
        Map<UUID, Lookup> cache = new HashMap<>();
        boolean budgetLimited = false;
        for (var candidate : selected) {
            // Always examine at least one row; resume after the last examined row.
            if (!rows.isEmpty() && System.nanoTime() - startedAt >= BATCH_BUDGET_NANOS) {
                budgetLimited = true;
                break;
            }
            var lookup = cache.computeIfAbsent(candidate.getShowtimeId(), this::lookup);
            if (lookup.endsAt() == null) {
                rows.add(
                        new Row(
                                candidate.getId(),
                                candidate.getShowtimeId(),
                                null,
                                lookup.result()));
                continue;
            }
            String result = "WOULD_UPDATE";
            if (!dryRun) {
                result =
                        transaction.execute(status -> {
                            var locked =
                                    repository.findByIdForUpdate(candidate.getId()).orElse(null);
                            if (locked == null) return "NOT_FOUND";
                            if (!locked.getShowtimeId().equals(candidate.getShowtimeId()))
                                return "SHOWTIME_CHANGED";
                            return locked.backfillShowtimeEnd(
                                            lookup.endsAt(), OffsetDateTime.now(clock), actor)
                                    ? "UPDATED"
                                    : "ALREADY_SET";
                        });
            }
            rows.add(
                    new Row(candidate.getId(), candidate.getShowtimeId(), lookup.endsAt(), result));
        }
        UUID next =
                (hasMore || budgetLimited) && !rows.isEmpty() ? rows.getLast().bookingId() : null;
        return new Batch(dryRun, next, List.copyOf(rows), budgetLimited);
    }

    private Lookup lookup(UUID id) {
        try {
            var showtime =
                    inventory
                            .get()
                            .uri("/api/v1/showtimes/{id}", id)
                            .retrieve()
                            .body(Showtime.class);
            if (showtime == null
                    || !id.equals(showtime.id())
                    || showtime.startsAt() == null
                    || showtime.endsAt() == null
                    || !showtime.endsAt().isAfter(showtime.startsAt())) {
                return new Lookup(null, "INVALID_SCHEDULE");
            }
            return new Lookup(showtime.endsAt(), "OK");
        } catch (RestClientResponseException failure) {
            return new Lookup(
                    null,
                    failure.getStatusCode().value() == 404
                            ? "SHOWTIME_NOT_FOUND"
                            : "INVENTORY_UNAVAILABLE");
        } catch (RestClientException failure) {
            return new Lookup(null, "INVENTORY_UNAVAILABLE");
        }
    }

    private record Showtime(UUID id, OffsetDateTime startsAt, OffsetDateTime endsAt) {}

    private record Lookup(OffsetDateTime endsAt, String result) {}

    public record Row(UUID bookingId, UUID showtimeId, OffsetDateTime endsAt, String result) {}

    public record Batch(boolean dryRun, UUID nextCursor, List<Row> rows, boolean budgetLimited) {}
}
