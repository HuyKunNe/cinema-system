package com.cinema.common.validation.validator;

import com.cinema.common.validation.UuidVersions;
import com.cinema.common.validation.annotation.UuidV5OrV7;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.UUID;

public class UuidV5OrV7Validator implements ConstraintValidator<UuidV5OrV7, UUID> {

    @Override
    public boolean isValid(UUID value, ConstraintValidatorContext context) {

        return value == null || UuidVersions.isV5OrV7(value);
    }
}
