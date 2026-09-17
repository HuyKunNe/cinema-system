package com.cinema.payment.service;

import com.cinema.payment.model.ReconciliationOperationResult;
import com.cinema.payment.model.ReconciliationRejectRequest;
import com.cinema.payment.model.ReconciliationResolveRequest;

public interface ReconciliationAdminService {

    ReconciliationOperationResult resolve(ReconciliationResolveRequest request);

    ReconciliationOperationResult reject(ReconciliationRejectRequest request);
}
