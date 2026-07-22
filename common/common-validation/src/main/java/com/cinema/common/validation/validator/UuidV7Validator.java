package com.cinema.common.validation.validator;

import java.util.UUID;

import com.cinema.common.validation.annotation.UuidV7;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UuidV7Validator
        implements ConstraintValidator<UuidV7, UUID> {

    @Override
    public boolean isValid(
            UUID value,
            ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        return value.version() == 7;

    }

}
