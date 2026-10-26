package com.cinema.common.exception.exception;

import com.cinema.common.exception.code.ErrorCode;

public class ServiceUnavailableException extends BusinessException {

    public ServiceUnavailableException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ServiceUnavailableException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
