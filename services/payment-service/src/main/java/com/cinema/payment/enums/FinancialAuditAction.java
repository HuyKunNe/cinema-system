package com.cinema.payment.enums;

public enum FinancialAuditAction {
    REFUND_REQUESTED,
    REFUND_SUCCEEDED,
    REFUND_FAILED,

    RECONCILIATION_OPENED,
    RECONCILIATION_RESOLVED,
    RECONCILIATION_REJECTED
}
