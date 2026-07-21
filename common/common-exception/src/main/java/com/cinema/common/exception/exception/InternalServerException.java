package com.cinema.common.exception.exception;

import com.cinema.common.exception.code.ErrorCode;

public class InternalServerException extends BusinessException {

    public InternalServerException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InternalServerException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
