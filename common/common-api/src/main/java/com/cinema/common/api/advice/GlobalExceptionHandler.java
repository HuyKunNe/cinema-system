package com.cinema.common.api.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cinema.common.api.mapper.ValidationErrorMapper;
import com.cinema.common.exception.code.ErrorCategory;
import com.cinema.common.exception.exception.BusinessException;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ForbiddenException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ResourceLockedException;
import com.cinema.common.exception.exception.UnauthorizedException;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.response.model.ErrorBody;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception) {

        return buildBusinessError(exception, resolveHttpStatus(exception.getErrorCode().category()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception) {

        var errors = ValidationErrorMapper.map(
                exception.getBindingResult()
                        .getFieldErrors());

        ErrorBody error = new ErrorBody(
                "VALIDATION_ERROR",
                "Validation failed",
                null,
                errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseFactory.error(error));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception) {

        ErrorBody error = new ErrorBody(
                "CONSTRAINT_VIOLATION",
                "Validation failed",
                exception.getMessage(),
                null);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseFactory.error(error));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception) {

        ErrorBody error = new ErrorBody(
                "INVALID_REQUEST_BODY",
                "Request body is invalid",
                ErrorCategory.VALIDATION.name(),
                null);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseFactory.error(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception exception) {

        ErrorBody error = new ErrorBody(
                "INTERNAL_SERVER_ERROR",
                "Unexpected error",
                null,
                null);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseFactory.error(error));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            NotFoundException exception) {

        return buildBusinessError(exception, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(
            ConflictException exception) {

        return buildBusinessError(exception, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(
            UnauthorizedException exception) {

        return buildBusinessError(exception, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(
            ForbiddenException exception) {

        return buildBusinessError(exception, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ResourceLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceLocked(
            ResourceLockedException exception) {

        return buildBusinessError(exception, HttpStatus.LOCKED);
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ApiResponse<Void>> handleInternalServer(
            InternalServerException exception) {

        return buildBusinessError(exception, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private HttpStatus resolveHttpStatus(
            ErrorCategory category) {

        return switch (category) {

            case VALIDATION ->
                HttpStatus.BAD_REQUEST;

            case BUSINESS ->
                HttpStatus.CONFLICT;

            case RESOURCE ->
                HttpStatus.NOT_FOUND;

            case SECURITY ->
                HttpStatus.UNAUTHORIZED;

            case SYSTEM ->
                HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private ResponseEntity<ApiResponse<Void>> buildBusinessError(
            BusinessException exception,
            HttpStatus status) {

        var errorCode = exception.getErrorCode();

        ErrorBody error = new ErrorBody(
                errorCode.code(),
                errorCode.message(),
                errorCode.category().name(),
                null);

        return ResponseEntity
                .status(status)
                .body(ResponseFactory.error(error));
    }
}
