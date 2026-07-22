package com.cinema.common.storage.model;

import java.io.InputStream;
import java.util.Map;

public record StorageUploadRequest(
        String objectName,
        InputStream inputStream,
        long size,
        String contentType,
        Map<String, String> metadata) {

    public StorageUploadRequest {

        if (objectName == null
                || objectName.isBlank()) {

            throw new IllegalArgumentException(
                    "Object name must not be blank");

        }

        if (inputStream == null) {

            throw new IllegalArgumentException(
                    "Input stream must not be null");

        }

        if (size < 0) {

            throw new IllegalArgumentException(
                    "Size must not be negative");

        }

        metadata = metadata == null
                ? Map.of()
                : Map.copyOf(metadata);

    }

    public static StorageUploadRequest of(
            String objectName,
            InputStream inputStream,
            long size,
            String contentType) {

        return new StorageUploadRequest(
                objectName,
                inputStream,
                size,
                contentType,
                Map.of());

    }

}
