package com.cinema.common.storage.exception;

public class StorageException
        extends RuntimeException {

    private final String objectName;

    public StorageException(
            String message) {

        this(
                message,
                null,
                null);

    }

    public StorageException(
            String message,
            Throwable cause) {

        this(
                message,
                null,
                cause);

    }

    public StorageException(
            String message,
            String objectName,
            Throwable cause) {

        super(
                message,
                cause);

        this.objectName = objectName;

    }

    public String getObjectName() {

        return objectName;

    }

}
