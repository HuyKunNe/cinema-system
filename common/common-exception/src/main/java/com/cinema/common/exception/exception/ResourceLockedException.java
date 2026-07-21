package com.cinema.common.exception.exception;

import com.cinema.common.exception.code.ErrorCode;

public class ResourceLockedException extends BusinessException {

    public ResourceLockedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceLockedException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
