package com.cinema.common.api.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cinema.common.api.mapper.ValidationErrorMapper;
import com.cinema.common.exception.code.ErrorCategory;
import com.cinema.common.exception.exception.BusinessException;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.response.model.ErrorBody;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception) {

        var errorCode = exception.getErrorCode();

        ErrorBody error = new ErrorBody(
                errorCode.code(),
                errorCode.message(),
                null,
                null);

        return ResponseEntity
                .status(resolveHttpStatus(errorCode.category()))
                .body(
                        ResponseFactory.error(error));
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
                .body(
                        ResponseFactory.error(error));
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
                .body(
                        ResponseFactory.error(error));
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
                .body(
                        ResponseFactory.error(error));
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
}
