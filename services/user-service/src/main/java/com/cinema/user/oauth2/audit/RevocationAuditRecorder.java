package com.cinema.user.oauth2.audit;

public interface RevocationAuditRecorder {

    void record(
            RevocationAuditTargetType targetType,
            String targetReference,
            RevocationReason reason,
            int revokedAuthorizationCount);
}
