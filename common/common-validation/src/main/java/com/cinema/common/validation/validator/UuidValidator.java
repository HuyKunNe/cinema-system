package com.cinema.common.validation.validator;

import com.cinema.common.validation.annotation.ValidUuid;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.UUID;

public class UuidValidator implements ConstraintValidator<ValidUuid, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null || value.isBlank()) {
            return true;
        }

        try {

            UUID.fromString(value);

            return true;

        } catch (Exception e) {

            return false;

        }

    }

}
