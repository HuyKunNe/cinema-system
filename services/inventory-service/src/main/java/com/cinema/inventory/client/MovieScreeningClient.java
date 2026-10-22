package com.cinema.inventory.client;

import com.cinema.common.api.screening.ScreeningFormat.ScreeningFormatOption;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class MovieScreeningClient {

    private final RestClient client;

    public MovieScreeningClient(
            RestClient.Builder builder,
            @Value("${cinema.inventory.movie-service.base-url:http://localhost:8081}")
                    String baseUrl) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        this.client =
                builder.clone()
                        .baseUrl(baseUrl)
                        .requestFactory(factory)
                        .build();
    }

    public MovieSnapshot get(UUID movieId) {
        MovieSnapshot snapshot;

        try {
            snapshot =
                    client.get()
                            .uri(
                                    "/api/v1/movies/{movieId}/screening-capabilities",
                                    movieId)
                            .retrieve()
                            .body(MovieSnapshot.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new NotFoundException(
                        InventoryErrorCode.SHOWTIME_MOVIE_NOT_FOUND);
            }

            throw unavailable();
        } catch (RestClientException exception) {
            throw unavailable();
        }

        if (snapshot == null
                || !movieId.equals(snapshot.movieId())
                || snapshot.version() == null
                || snapshot.version() < 0
                || snapshot.configuredFormats() == null) {
            throw unavailable();
        }

        for (ScreeningFormatOption option : snapshot.configuredFormats()) {
            if (option == null
                    || option.code() == null
                    || !option.code().toOption().equals(option)) {
                throw unavailable();
            }
        }

        return snapshot;
    }

    private static ConflictException unavailable() {
        return new ConflictException(
                InventoryErrorCode.SHOWTIME_MOVIE_UNAVAILABLE);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MovieSnapshot(
            UUID movieId,
            Long version,
            LocalDate releaseDate,
            String status,
            List<ScreeningFormatOption> configuredFormats) {}
}
