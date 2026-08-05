package com.cinema.common.security.web;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.cinema.common.security.error.SecurityErrorCode;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class CinemaAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final SecurityResponseWriter responseWriter;

    public CinemaAuthenticationEntryPoint(
            SecurityResponseWriter responseWriter) {

        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException)
            throws IOException, ServletException {

        responseWriter.write(
                response,
                HttpStatus.UNAUTHORIZED,
                SecurityErrorCode.AUTHENTICATION_REQUIRED);
    }
}
