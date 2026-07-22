package com.cinema.common.storage.model;

public record StorageUploadResult(
        String bucket,
        String objectName,
        String etag,
        String versionId) {

}
