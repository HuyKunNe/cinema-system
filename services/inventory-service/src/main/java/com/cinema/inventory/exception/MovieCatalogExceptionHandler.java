package com.cinema.inventory.exception;

import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.response.model.ErrorBody;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.cinema.inventory.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MovieCatalogExceptionHandler {

    @ExceptionHandler(MovieCatalogUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnavailable(
            MovieCatalogUnavailableException exception) {

        var code = exception.getErrorCode();

        ErrorBody error =
                new ErrorBody(
                        code.code(),
                        code.message(),
                        code.category().name(),
                        null);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ResponseFactory.error(error));
    }
}
