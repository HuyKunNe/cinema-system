package com.cinema.inventory.client;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.ShowtimeScreeningVersion;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.PresentationType;
import com.cinema.inventory.enums.ProjectionTechnology;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.exception.MovieCatalogUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.UnknownContentTypeException;

import javax.net.ssl.SSLException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Component
public class MovieCatalogClient {

    private static final Logger log = LoggerFactory.getLogger(MovieCatalogClient.class);

    private static final String REGISTRATION_ID = "movie-catalog";
    private static final String SERVICE_PRINCIPAL = "inventory-service";

    private static final Set<String> KNOWN_MOVIE_STATUSES =
            Set.of("UPCOMING", "NOW_SHOWING", "ENDED", "INACTIVE");

    private static final Set<String> SAFE_OAUTH_ERROR_CODES = Set.of(
            "invalid_client", "invalid_scope", "unauthorized_client", "unsupported_grant_type",
            "invalid_grant", "invalid_request", "invalid_token_response",
            "server_error", "temporarily_unavailable");

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
                        Operation.MOVIE_SCHEDULING,
                        movieId);

        if (response == null
                || !movieId.equals(response.id())
                || response.durationMinutes() == null
                || response.durationMinutes() < 1
                || response.version() == null
                || response.version() < 0
                || response.status() == null
                || !KNOWN_MOVIE_STATUSES.contains(response.status())) {
            throw unavailable(Operation.MOVIE_SCHEDULING, Failure.INVALID_MOVIE_RESPONSE, null, null);
        }

        if ("ENDED".equals(response.status()) || "INACTIVE".equals(response.status())) {
            throw new ConflictException(InventoryErrorCode.MOVIE_NOT_SCHEDULABLE);
        }

        return new MovieSchedulingDetails(
                response.durationMinutes(), response.version(), response.releaseDate());
    }

    public LocalDate getMovieReleaseDate(UUID movieId) {
        RemoteMovie response =
                get(
                        "/api/v1/movies/{movieId}",
                        RemoteMovie.class,
                        InventoryErrorCode.MOVIE_NOT_FOUND,
                        Operation.MOVIE_RELEASE_DATE,
                        movieId);

        if (response == null || !movieId.equals(response.id())) {
            throw unavailable(Operation.MOVIE_RELEASE_DATE, Failure.INVALID_MOVIE_RESPONSE, null, null);
        }

        return response.releaseDate();
    }

    public ShowtimeScreeningVersion getActiveVersion(UUID movieId, UUID versionId) {
        RemoteScreeningVersion response =
                get(
                        "/api/v1/movies/{movieId}/screening-versions/{versionId}",
                        RemoteScreeningVersion.class,
                        InventoryErrorCode.SCREENING_VERSION_NOT_FOUND,
                        Operation.SCREENING_VERSION,
                        movieId,
                        versionId);

        if (response == null
                || !movieId.equals(response.movieId())
                || !versionId.equals(response.id())
                || response.active() == null
                || response.presentationType() == null) {
            throw unavailable(Operation.SCREENING_VERSION, Failure.INVALID_SCREENING_VERSION_RESPONSE,
                    null, null);
        }

        if (!response.active()) {
            throw new ConflictException(InventoryErrorCode.SCREENING_VERSION_INACTIVE);
        }

        try {
            return new ShowtimeScreeningVersion(
                    response.id(),
                    response.version(),
                    ContentFormat.fromCode(response.contentFormat()),
                    response.projectionTechnology() == null
                            ? ProjectionTechnology.STANDARD
                            : ProjectionTechnology.valueOf(response.projectionTechnology()),
                    response.audioLanguage(),
                    PresentationType.valueOf(response.presentationType()),
                    response.subtitleLanguage());

        } catch (ValidationException | IllegalArgumentException exception) {
            throw unavailable(Operation.SCREENING_VERSION, Failure.INVALID_SCREENING_VERSION_DEFINITION,
                    null, null);
        }
    }

    private <T> T get(
            String path,
            Class<T> responseType,
            InventoryErrorCode notFoundCode,
            Operation operation,
            Object... uriVariables) {

        try {
            var authorizedClient =
                    authorizedClientManager.authorize(
                            OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID)
                                    .principal(SERVICE_PRINCIPAL)
                                    .build());

            if (authorizedClient == null) {
                throw unavailable(operation, Failure.NO_AUTHORIZED_CLIENT, null, null);
            }

            return restClient
                    .get()
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
                authorizedClientService.removeAuthorizedClient(REGISTRATION_ID, SERVICE_PRINCIPAL);
            }

            throw unavailable(operation, Failure.UPSTREAM_HTTP_ERROR,
                    exception.getStatusCode().value(), null);

        } catch (OAuth2AuthorizationException exception) {
            String code = exception.getError().getErrorCode();
            TokenDiagnostic diagnostic = tokenDiagnostic(exception);
            throw unavailable(operation, Failure.OAUTH2_AUTHORIZATION_FAILED, diagnostic.httpStatus(),
                    SAFE_OAUTH_ERROR_CODES.contains(code) ? code : "OTHER", diagnostic.failure());

        } catch (RestClientException exception) {
            throw unavailable(operation,
                    exception instanceof ResourceAccessException
                            ? Failure.UPSTREAM_TRANSPORT_FAILED
                            : Failure.UPSTREAM_RESPONSE_UNREADABLE,
                    null, null);
        }
    }

    private static MovieCatalogUnavailableException unavailable(
            Operation operation, Failure reason, Integer upstreamStatus, String oauthError) {
        return unavailable(operation, reason, upstreamStatus, oauthError, null);
    }

    private static MovieCatalogUnavailableException unavailable(
            Operation operation, Failure reason, Integer upstreamStatus, String oauthError,
            TokenFailure tokenFailure) {
        // Never log exception messages/causes, response bodies, headers, URLs or credentials here.
        log.warn("Movie catalog verification failed: operation={}, reason={}, upstreamStatus={}, oauthError={}, tokenFailure={}",
                operation, reason, upstreamStatus, oauthError, tokenFailure);
        return new MovieCatalogUnavailableException();
    }

    private static TokenDiagnostic tokenDiagnostic(OAuth2AuthorizationException exception) {
        Integer httpStatus = null;
        TokenFailure failure = TokenFailure.NO_CLASSIFIED_CAUSE;
        Throwable cause = exception.getCause();

        // Inspect types only, with a fixed bound; never emit arbitrary class names or messages.
        for (int depth = 0; cause != null && depth < 16; depth++, cause = cause.getCause()) {
            if (cause instanceof RestClientResponseException response) {
                httpStatus = response.getStatusCode().value();
                failure = TokenFailure.HTTP_RESPONSE_ERROR;
            } else if (cause instanceof HttpConnectTimeoutException) {
                failure = TokenFailure.CONNECT_TIMEOUT;
            } else if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                failure = TokenFailure.REQUEST_TIMEOUT;
            } else if (cause instanceof UnknownHostException || cause instanceof UnresolvedAddressException) {
                failure = TokenFailure.DNS_FAILURE;
            } else if (cause instanceof ConnectException) {
                failure = TokenFailure.CONNECTION_FAILED;
            } else if (cause instanceof SSLException) {
                failure = TokenFailure.TLS_FAILURE;
            } else if (cause instanceof UnknownContentTypeException) {
                failure = TokenFailure.UNSUPPORTED_CONTENT_TYPE;
            } else if (cause instanceof HttpMessageConversionException) {
                failure = TokenFailure.RESPONSE_CONVERSION_FAILED;
            } else if (cause instanceof ResourceAccessException) {
                failure = TokenFailure.TRANSPORT_FAILURE;
            } else if (cause instanceof RestClientException
                    && failure == TokenFailure.NO_CLASSIFIED_CAUSE) {
                failure = TokenFailure.REQUEST_OR_RESPONSE_FAILED;
            }
        }

        return new TokenDiagnostic(httpStatus, failure);
    }

    private record TokenDiagnostic(Integer httpStatus, TokenFailure failure) {}

    private enum TokenFailure {
        NO_CLASSIFIED_CAUSE,
        HTTP_RESPONSE_ERROR,
        CONNECT_TIMEOUT,
        REQUEST_TIMEOUT,
        DNS_FAILURE,
        CONNECTION_FAILED,
        TLS_FAILURE,
        UNSUPPORTED_CONTENT_TYPE,
        RESPONSE_CONVERSION_FAILED,
        TRANSPORT_FAILURE,
        REQUEST_OR_RESPONSE_FAILED
    }

    private enum Operation {
        MOVIE_SCHEDULING, MOVIE_RELEASE_DATE, SCREENING_VERSION
    }

    private enum Failure {
        NO_AUTHORIZED_CLIENT,
        OAUTH2_AUTHORIZATION_FAILED,
        UPSTREAM_HTTP_ERROR,
        UPSTREAM_TRANSPORT_FAILED,
        UPSTREAM_RESPONSE_UNREADABLE,
        INVALID_MOVIE_RESPONSE,
        INVALID_SCREENING_VERSION_RESPONSE,
        INVALID_SCREENING_VERSION_DEFINITION
    }

    public record MovieSchedulingDetails(
            Integer durationMinutes, Long version, LocalDate releaseDate) {

        public MovieSchedulingDetails(Integer durationMinutes, Long version) {

            this(durationMinutes, version, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteMovie(
            UUID id, Integer durationMinutes, String status, Long version, LocalDate releaseDate) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteScreeningVersion(
            UUID id,
            UUID movieId,
            String contentFormat,
            String projectionTechnology,
            String audioLanguage,
            String presentationType,
            String subtitleLanguage,
            Boolean active,
            Long version) {}
}
