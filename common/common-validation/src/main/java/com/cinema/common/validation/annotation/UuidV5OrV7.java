package com.cinema.common.validation.annotation;

import com.cinema.common.validation.validator.UuidV5OrV7Validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({
    ElementType.FIELD,
    ElementType.PARAMETER,
    ElementType.RECORD_COMPONENT,
    ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = UuidV5OrV7Validator.class)
public @interface UuidV5OrV7 {

    String message() default "Resource ID must be a UUID v5 or v7";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
