package com.cinema.common.storage.model;

import java.io.InputStream;

public record StorageDownload(
        String objectName,
        String contentType,
        long size,
        InputStream inputStream) implements AutoCloseable {

    public StorageDownload {

        if (objectName == null
                || objectName.isBlank()) {

            throw new IllegalArgumentException(
                    "Object name must not be blank");

        }

        if (inputStream == null) {

            throw new IllegalArgumentException(
                    "Input stream must not be null");

        }

    }

    @Override
    public void close()
            throws Exception {

        inputStream.close();

    }

}
