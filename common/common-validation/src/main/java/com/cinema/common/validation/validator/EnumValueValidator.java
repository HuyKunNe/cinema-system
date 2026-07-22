package com.cinema.common.validation.validator;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.cinema.common.validation.annotation.EnumValue;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EnumValueValidator
        implements ConstraintValidator<EnumValue, String> {

    private Set<String> acceptedValues;

    @Override
    public void initialize(EnumValue annotation) {

        acceptedValues = Arrays.stream(annotation.enumClass()
                .getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());

    }

    @Override
    public boolean isValid(
            String value,
            ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        return acceptedValues.contains(value);

    }

}
