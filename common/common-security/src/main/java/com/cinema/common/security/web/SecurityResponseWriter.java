package com.cinema.common.security.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.cinema.common.exception.code.ErrorCode;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.response.model.ErrorBody;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public final class SecurityResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper must not be null");
    }

    public void write(
            HttpServletResponse response,
            HttpStatus status,
            ErrorCode errorCode)
            throws IOException {

        ErrorBody error = new ErrorBody(
                errorCode.code(),
                errorCode.message(),
                errorCode.category().name(),
                null);

        ApiResponse<Void> body = ResponseFactory.error(error);

        response.setStatus(status.value());
        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name());
        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(
                response.getOutputStream(),
                body);
    }
}
