package com.cinema.payment.model;

import com.cinema.payment.enums.RefundStatus;

import java.util.UUID;

public record RefundRequestResult(
        UUID paymentId, UUID transactionId, RefundStatus refundStatus, boolean duplicate) {}
