package com.cinema.inventory.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.exception.MovieCatalogUnavailableException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpServerErrorException;

import javax.net.ssl.SSLException;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

class MovieCatalogClientDiagnosticsTest {

    private static final UUID MOVIE = UUID.fromString("03da36dd-7998-5759-ad29-302a7645a083");
    private static final UUID VERSION = UUID.fromString("01a1c383-28d4-71a3-96ff-5cd65f88e407");
    private static final String BASE_URL = "https://catalog.cinema.test";
    private static final String MOVIE_URL = BASE_URL + "/api/v1/movies/" + MOVIE;
    private static final String PRIVATE_MARKER = "sensitive-diagnostic-marker";

    private final OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
    private final OAuth2AuthorizedClientService clients = mock(OAuth2AuthorizedClientService.class);
    private final Logger logger = (Logger) LoggerFactory.getLogger(MovieCatalogClient.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private MockRestServiceServer server;
    private MovieCatalogClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MovieCatalogClient(builder.build(), manager, clients);
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        logs.stop();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid_client", "invalid_scope", PRIVATE_MARKER})
    void oauthFailuresLogOnlyAllowlistedCodes(String code) {
        when(manager.authorize(any())).thenThrow(new OAuth2AuthorizationException(
                new OAuth2Error(code, PRIVATE_MARKER, "https://identity.cinema.test/" + PRIVATE_MARKER)));

        var error = assertThrows(MovieCatalogUnavailableException.class,
                () -> client.getMovieForScheduling(MOVIE));

        assertEquals(InventoryErrorCode.MOVIE_CATALOG_UNAVAILABLE, error.getErrorCode());
        String message = singleLog();
        assertTrue(message.contains("operation=MOVIE_SCHEDULING"));
        assertTrue(message.contains("reason=OAUTH2_AUTHORIZATION_FAILED"));
        assertTrue(message.contains("oauthError=" + (code.equals(PRIVATE_MARKER) ? "OTHER" : code)));
        assertFalse(message.contains(PRIVATE_MARKER));
        assertFalse(message.contains("https://"));
        assertNull(logs.list.getFirst().getThrowableProxy());
        server.verify();
    }

    @Test
    void missingAuthorizedClientHasItsOwnSafeReason() {
        assertThrows(MovieCatalogUnavailableException.class, () -> client.getMovieForScheduling(MOVIE));
        assertTrue(singleLog().contains("reason=NO_AUTHORIZED_CLIENT"));
        server.verify();
    }

    @ParameterizedTest
    @MethodSource("tokenFailures")
    void classifiesWrappedTokenFailuresWithoutLoggingCauseData(
            Throwable cause, String classification, String httpStatus) {
        when(manager.authorize(any())).thenThrow(new OAuth2AuthorizationException(
                new OAuth2Error("invalid_token_response", PRIVATE_MARKER, null), cause));

        var error = assertThrows(MovieCatalogUnavailableException.class,
                () -> client.getMovieForScheduling(MOVIE));

        assertEquals(InventoryErrorCode.MOVIE_CATALOG_UNAVAILABLE, error.getErrorCode());
        String message = singleLog();
        assertTrue(message.contains("oauthError=invalid_token_response"));
        assertTrue(message.contains("tokenFailure=" + classification));
        assertTrue(message.contains("upstreamStatus=" + httpStatus));
        assertFalse(message.contains(PRIVATE_MARKER));
        assertFalse(message.contains(cause.getClass().getName()));
        assertNull(logs.list.getFirst().getThrowableProxy());
        server.verify();
    }

    @Test
    void tokenFailureWithoutCauseDoesNotInventRootCauseOrStatus() {
        when(manager.authorize(any())).thenThrow(new OAuth2AuthorizationException(
                new OAuth2Error("invalid_token_response", PRIVATE_MARKER, null)));

        assertThrows(MovieCatalogUnavailableException.class, () -> client.getMovieForScheduling(MOVIE));

        String message = singleLog();
        assertTrue(message.contains("tokenFailure=NO_CLASSIFIED_CAUSE"));
        assertTrue(message.contains("upstreamStatus=null"));
        assertFalse(message.contains(PRIVATE_MARKER));
    }

    private static Stream<Arguments> tokenFailures() {
        return Stream.of(
                Arguments.of(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, PRIVATE_MARKER),
                        "HTTP_RESPONSE_ERROR", "503"),
                Arguments.of(new ResourceAccessException(PRIVATE_MARKER, new ConnectException(PRIVATE_MARKER)),
                        "CONNECTION_FAILED", "null"),
                Arguments.of(new ResourceAccessException(PRIVATE_MARKER, new UnknownHostException(PRIVATE_MARKER)),
                        "DNS_FAILURE", "null"),
                Arguments.of(new ResourceAccessException(PRIVATE_MARKER, new HttpTimeoutException(PRIVATE_MARKER)),
                        "REQUEST_TIMEOUT", "null"),
                Arguments.of(new ResourceAccessException(PRIVATE_MARKER, new HttpConnectTimeoutException(PRIVATE_MARKER)),
                        "CONNECT_TIMEOUT", "null"),
                Arguments.of(new ResourceAccessException(PRIVATE_MARKER, new SSLException(PRIVATE_MARKER)),
                        "TLS_FAILURE", "null"),
                Arguments.of(new RestClientException(PRIVATE_MARKER, new HttpMessageConversionException(PRIVATE_MARKER)),
                        "RESPONSE_CONVERSION_FAILED", "null"));
    }

    @Test
    void upstreamUnauthorizedEvictsCachedClientWithoutLoggingResponseOrToken() {
        authorize();
        server.expect(requestTo(MOVIE_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body(PRIVATE_MARKER));

        assertThrows(MovieCatalogUnavailableException.class, () -> client.getMovieForScheduling(MOVIE));

        verify(clients).removeAuthorizedClient("movie-catalog", "inventory-service");
        String message = singleLog();
        assertTrue(message.contains("reason=UPSTREAM_HTTP_ERROR"));
        assertTrue(message.contains("upstreamStatus=401"));
        assertFalse(message.contains(PRIVATE_MARKER));
        server.verify();
    }

    @Test
    void upstreamNotFoundRetainsItsDomainError() {
        authorize();
        server.expect(requestTo(MOVIE_URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        var error = assertThrows(NotFoundException.class, () -> client.getMovieForScheduling(MOVIE));

        assertEquals(InventoryErrorCode.MOVIE_NOT_FOUND, error.getErrorCode());
        assertTrue(logs.list.isEmpty());
        server.verify();
    }

    @Test
    void unreadableBodyIsDistinguishedFromHttpError() {
        authorize();
        server.expect(requestTo(MOVIE_URL)).andRespond(withSuccess("{", MediaType.APPLICATION_JSON));

        assertThrows(MovieCatalogUnavailableException.class, () -> client.getMovieForScheduling(MOVIE));

        assertTrue(singleLog().contains("reason=UPSTREAM_RESPONSE_UNREADABLE"));
        server.verify();
    }

    @Test
    void validMovieAndScreeningPayloadsRemainAccepted() {
        authorize();
        server.expect(requestTo(MOVIE_URL)).andRespond(withSuccess("""
                {"id":"03da36dd-7998-5759-ad29-302a7645a083","durationMinutes":95,
                 "version":0,"status":"NOW_SHOWING","releaseDate":"2026-05-15"}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(MOVIE_URL + "/screening-versions/" + VERSION))
                .andRespond(withSuccess("""
                        {"id":"01a1c383-28d4-71a3-96ff-5cd65f88e407",
                         "movieId":"03da36dd-7998-5759-ad29-302a7645a083",
                         "contentFormat":"2D","projectionTechnology":"STANDARD",
                         "audioLanguage":"Original","presentationType":"NO_SUBTITLES",
                         "subtitleLanguage":null,"active":true,"version":0}
                        """, MediaType.APPLICATION_JSON));

        assertEquals(95, client.getMovieForScheduling(MOVIE).durationMinutes());
        var version = client.getActiveVersion(MOVIE, VERSION);
        assertEquals(ContentFormat.TWO_D, version.getContentFormat());
        assertEquals(VERSION, version.getId());
        assertTrue(logs.list.isEmpty());
        server.verify();
    }

    private void authorize() {
        var authorizedClient = mock(OAuth2AuthorizedClient.class);
        when(authorizedClient.getAccessToken()).thenReturn(new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, PRIVATE_MARKER,
                Instant.parse("2026-11-10T00:00:00Z"), Instant.parse("2026-11-11T00:00:00Z")));
        when(manager.authorize(any())).thenReturn(authorizedClient);
    }

    private String singleLog() {
        assertEquals(1, logs.list.size());
        return logs.list.getFirst().getFormattedMessage();
    }
}
