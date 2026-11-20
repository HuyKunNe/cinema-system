package com.cinema.booking.client;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ForbiddenException;
import com.cinema.common.exception.exception.ServiceUnavailableException;
import com.cinema.common.security.authentication.CurrentUser;
import com.cinema.common.security.context.SecurityContextUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Component
public class PrintSimulationAccessClient {

    private final RestClient users;
    private final RestClient inventory;

    public PrintSimulationAccessClient(
            @Value("${cinema.booking.user-base-url:http://localhost:8082}") String userBaseUrl,
            @Value("${cinema.booking.inventory-base-url:http://localhost:8083}")
                    String inventoryBaseUrl) {
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(1))
                                .followRedirects(HttpClient.Redirect.NEVER)
                                .build());
        factory.setReadTimeout(Duration.ofSeconds(2));
        users = RestClient.builder().baseUrl(userBaseUrl).requestFactory(factory).build();
        inventory = RestClient.builder().baseUrl(inventoryBaseUrl).requestFactory(factory).build();
    }

    public Access current() {
        if (!(SecurityContextUtils.getAuthentication() instanceof JwtAuthenticationToken jwt)) {
            throw new ForbiddenException(BookingErrorCode.PRINT_ACCESS_DENIED);
        }
        Access access;
        try {
            access =
                    users.get()
                            .uri("/api/v1/users/me/staff-context")
                            .headers(headers ->
                                    headers.setBearerAuth(jwt.getToken().getTokenValue()))
                            .retrieve()
                            .body(Access.class);
        } catch (RestClientResponseException failure) {
            if (failure.getStatusCode().value() == 401 || failure.getStatusCode().value() == 403) {
                throw new ForbiddenException(BookingErrorCode.PRINT_ACCESS_DENIED);
            }
            throw new ServiceUnavailableException(BookingErrorCode.PRINT_DEPENDENCY_UNAVAILABLE);
        } catch (RestClientException failure) {
            throw new ServiceUnavailableException(BookingErrorCode.PRINT_DEPENDENCY_UNAVAILABLE);
        }
        if (access == null
                || !CurrentUser.id().equals(access.userId())
                || access.administrator() == null
                || (!access.administrator() && access.cinemaId() == null)) {
            throw new ForbiddenException(BookingErrorCode.PRINT_ACCESS_DENIED);
        }
        return access;
    }

    public Schedule schedule(UUID showtimeId) {
        Schedule schedule;
        try {
            schedule =
                    inventory
                            .get()
                            .uri("/api/v1/showtimes/{id}", showtimeId)
                            .retrieve()
                            .body(Schedule.class);
        } catch (RestClientException failure) {
            throw new ServiceUnavailableException(BookingErrorCode.PRINT_DEPENDENCY_UNAVAILABLE);
        }
        if (schedule == null
                || !showtimeId.equals(schedule.id())
                || schedule.cinemaId() == null
                || schedule.startsAt() == null
                || schedule.endsAt() == null
                || !schedule.endsAt().isAfter(schedule.startsAt())
                || schedule.status() == null) {
            throw new ServiceUnavailableException(BookingErrorCode.PRINT_DEPENDENCY_UNAVAILABLE);
        }
        if (!Set.of("SCHEDULED", "OPEN_FOR_BOOKING", "CLOSED").contains(schedule.status())) {
            throw new ConflictException(BookingErrorCode.PRINT_NOT_ALLOWED);
        }
        Cinema cinema;
        try {
            cinema =
                    inventory
                            .get()
                            .uri("/api/v1/cinemas/{id}", schedule.cinemaId())
                            .retrieve()
                            .body(Cinema.class);
        } catch (RestClientException failure) {
            throw new ServiceUnavailableException(BookingErrorCode.PRINT_DEPENDENCY_UNAVAILABLE);
        }
        if (cinema == null || !schedule.cinemaId().equals(cinema.id()) || cinema.active() == null) {
            throw new ServiceUnavailableException(BookingErrorCode.PRINT_DEPENDENCY_UNAVAILABLE);
        }
        if (!cinema.active()) {
            throw new ConflictException(BookingErrorCode.PRINT_NOT_ALLOWED);
        }
        return schedule;
    }

    public record Access(UUID userId, Boolean administrator, UUID cinemaId) {
        public void requireCinema(UUID expected) {
            if (!Boolean.TRUE.equals(administrator) && !expected.equals(cinemaId)) {
                throw new ForbiddenException(BookingErrorCode.PRINT_ACCESS_DENIED);
            }
        }
    }

    public record Schedule(
            UUID id,
            UUID cinemaId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String status) {}

    private record Cinema(UUID id, Boolean active) {}
}
