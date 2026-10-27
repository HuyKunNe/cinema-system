package com.cinema.inventory.controller.advice;

import com.cinema.common.api.advice.GlobalExceptionHandler;
import com.cinema.common.exception.exception.BusinessException;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ForbiddenException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ResourceLockedException;
import com.cinema.common.exception.exception.ServiceUnavailableException;
import com.cinema.common.exception.exception.UnauthorizedException;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.response.model.ErrorBody;
import com.cinema.common.response.model.ValidationError;
import com.cinema.inventory.controller.ShowtimeController;
import com.cinema.inventory.exception.ShowtimeBatchItemException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Objects;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ShowtimeController.class)
public class ShowtimeBatchExceptionHandler {

    private final GlobalExceptionHandler globalHandler;

    public ShowtimeBatchExceptionHandler(
            GlobalExceptionHandler globalHandler) {

        this.globalHandler = globalHandler;
    }

    @ExceptionHandler(ShowtimeBatchItemException.class)
    public ResponseEntity<ApiResponse<Void>> handleBatchItem(
            ShowtimeBatchItemException exception) {

        ResponseEntity<ApiResponse<Void>> originalResponse =
                handleOriginal(exception.getOriginal());

        ErrorBody originalError =
                Objects.requireNonNull(originalResponse.getBody()).error();

        ErrorBody error =
                new ErrorBody(
                        originalError.code(),
                        originalError.message(),
                        originalError.category(),
                        List.of(
                                new ValidationError(
                                        exception.getFieldPath(),
                                        originalError.message())));

        return ResponseEntity.status(originalResponse.getStatusCode())
                .body(ResponseFactory.error(error));
    }

    private ResponseEntity<ApiResponse<Void>> handleOriginal(
            BusinessException original) {

        return switch (original) {
            case NotFoundException exception ->
                    globalHandler.handleNotFound(exception);

            case ConflictException exception ->
                    globalHandler.handleConflict(exception);

            case UnauthorizedException exception ->
                    globalHandler.handleUnauthorized(exception);

            case ForbiddenException exception ->
                    globalHandler.handleForbidden(exception);

            case ResourceLockedException exception ->
                    globalHandler.handleResourceLocked(exception);

            case InternalServerException exception ->
                    globalHandler.handleInternalServer(exception);

            case ServiceUnavailableException exception ->
                    globalHandler.handleServiceUnavailable(exception);

            default ->
                    globalHandler.handleBusinessException(original);
        };
    }
}
