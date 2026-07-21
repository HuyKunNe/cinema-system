package com.cinema.common.api.mapper;

import com.cinema.common.response.model.ValidationError;

import org.springframework.validation.FieldError;

import java.util.List;

public final class ValidationErrorMapper {

    private ValidationErrorMapper() {
    }

    public static List<ValidationError> map(
            List<FieldError> errors) {

        return errors.stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        error.getDefaultMessage()))
                .toList();
    }
}
