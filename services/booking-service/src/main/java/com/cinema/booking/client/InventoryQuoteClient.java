package com.cinema.booking.client;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.api.pricing.BookingQuoteResponse;
import com.cinema.common.api.pricing.CreateBookingQuoteRequest;
import com.cinema.common.exception.code.ErrorCategory;
import com.cinema.common.exception.code.ErrorCode;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ForbiddenException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.UnauthorizedException;
import com.cinema.common.exception.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class InventoryQuoteClient {

    private final RestClient client;
    private final ObjectMapper objectMapper;

    public InventoryQuoteClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${cinema.booking.inventory-service.base-url:http://localhost:8083}")
                    String baseUrl) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        this.client = builder.clone().baseUrl(baseUrl).requestFactory(factory).build();

        this.objectMapper = objectMapper;
    }

    public BookingQuoteResponse create(CreateBookingQuoteRequest request, String authorization) {

        BookingQuoteResponse response;

        try {
            response =
                    client.post()
                            .uri("/api/v1/booking-quotes")
                            .header(HttpHeaders.AUTHORIZATION, authorization)
                            .body(request)
                            .retrieve()
                            .body(BookingQuoteResponse.class);
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        } catch (RestClientException exception) {
            throw unavailable();
        }

        if (response == null
                || response.quoteId() == null
                || !request.showtimeId().equals(response.showtimeId())
                || response.items() == null
                || response.items().size() != request.items().size()) {
            throw unavailable();
        }

        return response;
    }

    public BookingQuoteResponse get(UUID quoteId, String authorization) {
        BookingQuoteResponse response;

        try {
            response =
                    client.get()
                            .uri("/api/v1/booking-quotes/{quoteId}", quoteId)
                            .header(HttpHeaders.AUTHORIZATION, authorization)
                            .retrieve()
                            .body(BookingQuoteResponse.class);
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        } catch (RestClientException exception) {
            throw unavailable();
        }

        if (response == null
                || !quoteId.equals(response.quoteId())
                || response.showtimeId() == null
                || response.items() == null
                || response.items().isEmpty()) {
            throw unavailable();
        }

        return response;
    }

    private RuntimeException translate(RestClientResponseException exception) {

        int status = exception.getStatusCode().value();

        if (status == 401) {
            return new UnauthorizedException(BookingErrorCode.QUOTE_AUTHENTICATION_REQUIRED);
        }

        if (status == 403) {
            return new ForbiddenException(BookingErrorCode.QUOTE_ACCESS_DENIED);
        }

        if (status != 400 && status != 404 && status != 409) {
            return unavailable();
        }

        try {
            JsonNode root = objectMapper.readTree(exception.getResponseBodyAsString());

            if (root == null) {
                return unavailable();
            }

            JsonNode error = root.path("error");

            String code = error.path("code").asText("");
            String message = error.path("message").asText("");
            String category = error.path("category").asText("");

            if (!code.matches("INVENTORY_[A-Z0-9_]{1,100}")
                    || message.isBlank()
                    || message.length() > 1000) {
                return unavailable();
            }

            ErrorCode remoteCode =
                    new RemoteErrorCode(code, message, ErrorCategory.valueOf(category));

            return switch (status) {
                case 400 -> new ValidationException(remoteCode);
                case 404 -> new NotFoundException(remoteCode);
                case 409 -> new ConflictException(remoteCode);
                default -> unavailable();
            };
        } catch (JsonProcessingException | IllegalArgumentException exceptionParsing) {
            return unavailable();
        }
    }

    private static InternalServerException unavailable() {
        return new InternalServerException(BookingErrorCode.QUOTE_SERVICE_UNAVAILABLE);
    }

    private record RemoteErrorCode(String code, String message, ErrorCategory category)
            implements ErrorCode {}
}
