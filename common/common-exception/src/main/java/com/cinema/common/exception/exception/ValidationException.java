package com.cinema.common.exception.exception;

import com.cinema.common.exception.code.ErrorCode;

public class ValidationException extends BusinessException {

    public ValidationException(
            ErrorCode errorCode) {
        super(errorCode);
    }

    public ValidationException(
            ErrorCode errorCode,
            Throwable cause) {
        super(errorCode, cause);
    }
}
