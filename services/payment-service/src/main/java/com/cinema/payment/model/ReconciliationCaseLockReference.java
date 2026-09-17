package com.cinema.payment.model;

import java.util.UUID;

public record ReconciliationCaseLockReference(UUID paymentId, UUID paymentTransactionId) {}
