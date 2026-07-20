package com.cinema.common.exception.exception;

import com.cinema.common.exception.code.ErrorCode;

public class BusinessException extends BaseException {

    public BusinessException(ErrorCode errorCode) {

        super(errorCode);

    }

}
