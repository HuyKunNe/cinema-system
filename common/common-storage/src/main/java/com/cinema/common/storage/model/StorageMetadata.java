package com.cinema.common.storage.model;

import java.time.Instant;
import java.util.Map;

public record StorageMetadata(
        String bucket,
        String objectName,
        long size,
        String contentType,
        String etag,
        Instant lastModified,
        Map<String, String> userMetadata) {

    public StorageMetadata {

        userMetadata = userMetadata == null
                ? Map.of()
                : Map.copyOf(userMetadata);

    }

}
