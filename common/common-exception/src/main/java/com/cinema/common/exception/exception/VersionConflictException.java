package com.cinema.common.exception.exception;

import com.cinema.common.exception.code.ErrorCode;

/** A conflict with the resource version observed while checking the write precondition. */
public final class VersionConflictException extends ConflictException {

    private final long currentVersion;

    public VersionConflictException(ErrorCode errorCode, long currentVersion) {
        super(errorCode);
        this.currentVersion = currentVersion;
    }

    public long getCurrentVersion() {
        return currentVersion;
    }
}
