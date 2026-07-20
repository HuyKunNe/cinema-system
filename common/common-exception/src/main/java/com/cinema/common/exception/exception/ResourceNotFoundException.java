package com.cinema.common.exception.exception;

import com.cinema.common.exception.code.ErrorCode;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(ErrorCode errorCode) {

        super(errorCode);

    }

}
