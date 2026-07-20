package com.cinema.common.exception.exception;

import com.cinema.common.exception.code.ErrorCode;

import lombok.Getter;

@Getter
public abstract class BaseException
        extends RuntimeException {

    private final ErrorCode errorCode;

    protected BaseException(ErrorCode errorCode) {

        super(errorCode.getMessage());

        this.errorCode = errorCode;

    }

}
