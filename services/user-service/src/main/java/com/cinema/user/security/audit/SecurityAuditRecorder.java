package com.cinema.user.security.audit;

public interface SecurityAuditRecorder {

    void record(SecurityAuditRecord record);
}
