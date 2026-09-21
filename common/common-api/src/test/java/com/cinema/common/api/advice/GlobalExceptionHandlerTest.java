package com.cinema.common.api.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.cinema.common.response.model.ApiResponse;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void shouldReturnNotFoundForMissingStaticResource() {

        NoResourceFoundException exception =
                new NoResourceFoundException(
                        HttpMethod.GET,
                        "favicon.ico");

        var response = handler.handleNoResourceFound(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ApiResponse<Void> body = response.getBody();

        assertNotNull(body);
        assertFalse(body.success());
        assertNull(body.data());
        assertNotNull(body.error());
        assertEquals("RESOURCE_NOT_FOUND", body.error().code());
        assertEquals("Resource not found", body.error().message());
        assertEquals("RESOURCE", body.error().category());
        assertNull(body.error().details());
    }
}
