package com.cinema.user.client;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ServiceUnavailableException;
import com.cinema.user.exception.UserErrorCode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

@Component
public class StaffCinemaCatalogClient {

    private final RestClient client;

    public StaffCinemaCatalogClient(
            @Value("${cinema.user.inventory-base-url:http://localhost:8083}") String baseUrl) {
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(1))
                                .followRedirects(HttpClient.Redirect.NEVER)
                                .build());
        factory.setReadTimeout(Duration.ofSeconds(2));
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public void requireActiveCinema(UUID cinemaId) {
        Cinema cinema;
        try {
            cinema =
                    client.get()
                            .uri("/api/v1/cinemas/{id}", cinemaId)
                            .retrieve()
                            .body(Cinema.class);
        } catch (RestClientResponseException failure) {
            if (failure.getStatusCode().value() == 404) {
                throw new NotFoundException(UserErrorCode.STAFF_CINEMA_NOT_FOUND);
            }
            throw new ServiceUnavailableException(UserErrorCode.STAFF_CINEMA_UNAVAILABLE);
        } catch (RestClientException failure) {
            throw new ServiceUnavailableException(UserErrorCode.STAFF_CINEMA_UNAVAILABLE);
        }
        if (cinema == null || !cinemaId.equals(cinema.id()) || cinema.active() == null) {
            throw new ServiceUnavailableException(UserErrorCode.STAFF_CINEMA_UNAVAILABLE);
        }
        if (!cinema.active()) {
            throw new ConflictException(UserErrorCode.STAFF_CINEMA_INACTIVE);
        }
    }

    private record Cinema(UUID id, Boolean active) {}
}
