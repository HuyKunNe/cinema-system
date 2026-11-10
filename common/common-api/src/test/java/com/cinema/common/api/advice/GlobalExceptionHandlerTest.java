package com.cinema.common.api.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.cinema.common.exception.code.ErrorCategory;
import com.cinema.common.exception.code.ErrorCode;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.VersionConflictException;
import com.cinema.common.response.model.ApiResponse;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private static final ErrorCode VERSION_CONFLICT =
            new TestErrorCode("VERSION_CONFLICT", "Resource version has changed", ErrorCategory.BUSINESS);

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void shouldReturnCurrentVersionForVersionConflict() {

        var response = handler.handleConflict(new VersionConflictException(VERSION_CONFLICT, 7L));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        var body = response.getBody();
        assertNotNull(body);
        assertFalse(body.success());
        assertNull(body.data());
        assertEquals(VERSION_CONFLICT.code(), body.error().code());
        assertEquals(VERSION_CONFLICT.message(), body.error().message());
        assertEquals("BUSINESS", body.error().category());
        assertEquals(Long.valueOf(7L), body.error().currentVersion());
        assertNull(body.error().details());
    }

    @Test
    void shouldNotInventCurrentVersionForOrdinaryConflict() {

        var response = handler.handleConflict(new ConflictException(VERSION_CONFLICT));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        var body = response.getBody();
        assertNotNull(body);
        assertEquals(VERSION_CONFLICT.code(), body.error().code());
        assertNull(body.error().currentVersion());
    }

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
    private record TestErrorCode(String code, String message, ErrorCategory category)
            implements ErrorCode {}
}
