package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.code.ErrorCode;

public class QuoteReservationRejectedException extends RuntimeException {

    private final ErrorCode errorCode;

    public QuoteReservationRejectedException(
            ErrorCode errorCode,
            Throwable cause) {

        super(errorCode.message(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
