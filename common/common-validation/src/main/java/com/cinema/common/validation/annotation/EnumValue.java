package com.cinema.common.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.cinema.common.validation.constant.ValidationConstants;
import com.cinema.common.validation.validator.EnumValueValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({
        ElementType.FIELD,
        ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = EnumValueValidator.class)
public @interface EnumValue {

    Class<? extends Enum<?>> enumClass();

    String message() default ValidationConstants.INVALID_ENUM;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
