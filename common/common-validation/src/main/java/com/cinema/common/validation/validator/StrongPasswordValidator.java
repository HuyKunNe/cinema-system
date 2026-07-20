package com.cinema.common.validation.validator;

import com.cinema.common.validation.annotation.StrongPassword;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {

        if (password == null) {
            return true;
        }

        return password.matches(
                "^(?=.*[a-z])"
                        + "(?=.*[A-Z])"
                        + "(?=.*\\d)"
                        + "(?=.*[@#$%^&+=])"
                        + ".{8,}$");

    }

}
