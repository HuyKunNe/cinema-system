package com.cinema.common.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.cinema.common.validation.constant.ValidationConstants;
import com.cinema.common.validation.validator.UuidV7Validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({
        ElementType.FIELD,
        ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = UuidV7Validator.class)
public @interface UuidV7 {

    String message() default ValidationConstants.INVALID_UUID_V7;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
