package com.cinema.payment.service;

import com.cinema.payment.model.RefundRequest;
import com.cinema.payment.model.RefundRequestResult;

public interface RefundRequestService {

    RefundRequestResult requestRefund(RefundRequest request);
}
