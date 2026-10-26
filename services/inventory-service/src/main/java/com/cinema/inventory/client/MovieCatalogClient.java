package com.cinema.inventory.client;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.ShowtimeScreeningVersion;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.PresentationType;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.exception.MovieCatalogUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Set;
import java.util.UUID;

@Component
public class MovieCatalogClient {

    private static final String REGISTRATION_ID = "movie-catalog";
    private static final String SERVICE_PRINCIPAL = "inventory-service";

    private static final Set<String> KNOWN_MOVIE_STATUSES =
            Set.of("UPCOMING", "NOW_SHOWING", "ENDED", "INACTIVE");

    private final RestClient restClient;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public MovieCatalogClient(
            @Qualifier("movieCatalogRestClient") RestClient restClient,
            @Qualifier("movieCatalogAuthorizedClientManager")
                    OAuth2AuthorizedClientManager authorizedClientManager,
            OAuth2AuthorizedClientService authorizedClientService) {

        this.restClient = restClient;
        this.authorizedClientManager = authorizedClientManager;
        this.authorizedClientService = authorizedClientService;
    }

    public MovieSchedulingDetails getMovieForScheduling(UUID movieId) {
        RemoteMovie response =
                get(
                        "/api/v1/movies/{movieId}",
                        RemoteMovie.class,
                        InventoryErrorCode.MOVIE_NOT_FOUND,
                        movieId);

        if (response == null
                || !movieId.equals(response.id())
                || response.durationMinutes() == null
                || response.durationMinutes() < 1
                || response.version() == null
                || response.version() < 0
                || response.status() == null
                || !KNOWN_MOVIE_STATUSES.contains(response.status())) {
            throw new MovieCatalogUnavailableException();
        }

        if ("ENDED".equals(response.status())
                || "INACTIVE".equals(response.status())) {
            throw new ConflictException(
                    InventoryErrorCode.MOVIE_NOT_SCHEDULABLE);
        }

        return new MovieSchedulingDetails(
                response.durationMinutes(), response.version());
    }

    public ShowtimeScreeningVersion getActiveVersion(UUID movieId, UUID versionId) {
        RemoteScreeningVersion response =
                get(
                        "/api/v1/movies/{movieId}/screening-versions/{versionId}",
                        RemoteScreeningVersion.class,
                        InventoryErrorCode.SCREENING_VERSION_NOT_FOUND,
                        movieId,
                        versionId);

        if (response == null
                || !movieId.equals(response.movieId())
                || !versionId.equals(response.id())
                || response.active() == null
                || response.presentationType() == null) {
            throw new MovieCatalogUnavailableException();
        }

        if (!response.active()) {
            throw new ConflictException(
                    InventoryErrorCode.SCREENING_VERSION_INACTIVE);
        }

        try {
            return new ShowtimeScreeningVersion(
                    response.id(),
                    response.version(),
                    ContentFormat.fromCode(response.contentFormat()),
                    response.audioLanguage(),
                    PresentationType.valueOf(response.presentationType()),
                    response.subtitleLanguage());

        } catch (ValidationException | IllegalArgumentException exception) {
            throw new MovieCatalogUnavailableException();
        }
    }

    private <T> T get(
            String path,
            Class<T> responseType,
            InventoryErrorCode notFoundCode,
            Object... uriVariables) {

        try {
            var authorizedClient =
                    authorizedClientManager.authorize(
                            OAuth2AuthorizeRequest
                                    .withClientRegistrationId(REGISTRATION_ID)
                                    .principal(SERVICE_PRINCIPAL)
                                    .build());

            if (authorizedClient == null) {
                throw new MovieCatalogUnavailableException();
            }

            return restClient.get()
                    .uri(path, uriVariables)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(
                            headers ->
                                    headers.setBearerAuth(
                                            authorizedClient.getAccessToken().getTokenValue()))
                    .retrieve()
                    .body(responseType);

        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new NotFoundException(notFoundCode);
            }

            if (exception.getStatusCode().value() == 401) {
                authorizedClientService.removeAuthorizedClient(
                        REGISTRATION_ID, SERVICE_PRINCIPAL);
            }

            throw new MovieCatalogUnavailableException();

        } catch (RestClientException | OAuth2AuthorizationException exception) {
            throw new MovieCatalogUnavailableException();
        }
    }

    public record MovieSchedulingDetails(
            Integer durationMinutes,
            Long version) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteMovie(
            UUID id,
            Integer durationMinutes,
            String status,
            Long version) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteScreeningVersion(
            UUID id,
            UUID movieId,
            String contentFormat,
            String audioLanguage,
            String presentationType,
            String subtitleLanguage,
            Boolean active,
            Long version) {}
}
