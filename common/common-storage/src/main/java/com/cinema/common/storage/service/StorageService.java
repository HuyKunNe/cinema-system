package com.cinema.common.storage.service;

import java.time.Duration;

import com.cinema.common.storage.model.StorageDownload;
import com.cinema.common.storage.model.StorageMetadata;
import com.cinema.common.storage.model.StorageUploadRequest;
import com.cinema.common.storage.model.StorageUploadResult;

public interface StorageService {

    StorageUploadResult upload(
            StorageUploadRequest request);

    StorageDownload download(
            String objectName);

    StorageMetadata getMetadata(
            String objectName);

    boolean exists(
            String objectName);

    void delete(
            String objectName);

    String createPresignedDownloadUrl(
            String objectName);

    String createPresignedDownloadUrl(
            String objectName,
            Duration expiration);

}
