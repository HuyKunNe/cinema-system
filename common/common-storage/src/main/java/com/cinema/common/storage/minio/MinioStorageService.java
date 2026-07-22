package com.cinema.common.storage.minio;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.cinema.common.storage.exception.StorageException;
import com.cinema.common.storage.model.StorageDownload;
import com.cinema.common.storage.model.StorageMetadata;
import com.cinema.common.storage.model.StorageUploadRequest;
import com.cinema.common.storage.model.StorageUploadResult;
import com.cinema.common.storage.properties.StorageProperties;
import com.cinema.common.storage.service.StorageService;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;

public class MinioStorageService
        implements StorageService {

    private static final int DEFAULT_PART_SIZE = -1;

    private final MinioClient minioClient;

    private final StorageProperties properties;

    public MinioStorageService(
            MinioClient minioClient,
            StorageProperties properties) {

        this.minioClient = Objects.requireNonNull(
                minioClient,
                "MinioClient must not be null");

        this.properties = Objects.requireNonNull(
                properties,
                "StorageProperties must not be null");

    }

    @Override
    public StorageUploadResult upload(
            StorageUploadRequest request) {

        Objects.requireNonNull(
                request,
                "Upload request must not be null");

        try {

            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(
                            getBucket())
                    .object(
                            normalizeObjectName(
                                    request.objectName()))
                    .stream(
                            request.inputStream(),
                            request.size(),
                            DEFAULT_PART_SIZE)
                    .userMetadata(
                            request.metadata());

            if (request.contentType() != null
                    && !request.contentType().isBlank()) {

                builder.contentType(
                        request.contentType());

            }

            ObjectWriteResponse response = minioClient.putObject(
                    builder.build());

            return new StorageUploadResult(
                    response.bucket(),
                    response.object(),
                    response.etag(),
                    response.versionId());

        } catch (Exception exception) {

            throw storageException(
                    "Cannot upload object",
                    request.objectName(),
                    exception);

        }

    }

    @Override
    public StorageDownload download(
            String objectName) {

        String normalizedObjectName = normalizeObjectName(
                objectName);

        try {

            StatObjectResponse metadata = statObject(
                    normalizedObjectName);

            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(
                                    getBucket())
                            .object(
                                    normalizedObjectName)
                            .build());

            return new StorageDownload(
                    normalizedObjectName,
                    metadata.contentType(),
                    metadata.size(),
                    response);

        } catch (Exception exception) {

            throw storageException(
                    "Cannot download object",
                    normalizedObjectName,
                    exception);

        }

    }

    @Override
    public StorageMetadata getMetadata(
            String objectName) {

        String normalizedObjectName = normalizeObjectName(
                objectName);

        try {

            StatObjectResponse response = statObject(
                    normalizedObjectName);

            Instant lastModified = response.lastModified() == null
                    ? null
                    : response.lastModified()
                            .toInstant();

            return new StorageMetadata(
                    response.bucket(),
                    response.object(),
                    response.size(),
                    response.contentType(),
                    response.etag(),
                    lastModified,
                    response.userMetadata());

        } catch (Exception exception) {

            throw storageException(
                    "Cannot retrieve object metadata",
                    normalizedObjectName,
                    exception);

        }

    }

    @Override
    public boolean exists(
            String objectName) {

        String normalizedObjectName = normalizeObjectName(
                objectName);

        try {

            statObject(
                    normalizedObjectName);

            return true;

        } catch (ErrorResponseException exception) {

            String code = exception.errorResponse()
                    .code();

            if ("NoSuchKey".equals(code)
                    || "NoSuchObject".equals(code)
                    || "NotFound".equals(code)) {

                return false;

            }

            throw storageException(
                    "Cannot check object existence",
                    normalizedObjectName,
                    exception);

        } catch (Exception exception) {

            throw storageException(
                    "Cannot check object existence",
                    normalizedObjectName,
                    exception);

        }

    }

    @Override
    public void delete(
            String objectName) {

        String normalizedObjectName = normalizeObjectName(
                objectName);

        try {

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(
                                    getBucket())
                            .object(
                                    normalizedObjectName)
                            .build());

        } catch (Exception exception) {

            throw storageException(
                    "Cannot delete object",
                    normalizedObjectName,
                    exception);

        }

    }

    @Override
    public String createPresignedDownloadUrl(
            String objectName) {

        return createPresignedDownloadUrl(
                objectName,
                properties.getPresignedUrlExpiration());

    }

    @Override
    public String createPresignedDownloadUrl(
            String objectName,
            Duration expiration) {

        String normalizedObjectName = normalizeObjectName(
                objectName);

        validateExpiration(
                expiration);

        try {

            long seconds = expiration.toSeconds();

            if (seconds > Integer.MAX_VALUE) {

                throw new IllegalArgumentException(
                        "Presigned URL expiration is too large");

            }

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(
                                    Method.GET)
                            .bucket(
                                    getBucket())
                            .object(
                                    normalizedObjectName)
                            .expiry(
                                    (int) seconds,
                                    TimeUnit.SECONDS)
                            .build());

        } catch (Exception exception) {

            throw storageException(
                    "Cannot create presigned download URL",
                    normalizedObjectName,
                    exception);

        }

    }

    private StatObjectResponse statObject(
            String objectName)
            throws Exception {

        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(
                                getBucket())
                        .object(
                                objectName)
                        .build());

    }

    private String getBucket() {

        String bucket = properties.getBucket();

        if (bucket == null
                || bucket.isBlank()) {

            throw new StorageException(
                    "Storage bucket must not be blank");

        }

        return bucket.trim();

    }

    private String normalizeObjectName(
            String objectName) {

        if (objectName == null
                || objectName.isBlank()) {

            throw new IllegalArgumentException(
                    "Object name must not be blank");

        }

        String normalized = objectName.trim()
                .replace('\\', '/');

        while (normalized.startsWith("/")) {

            normalized = normalized.substring(1);

        }

        if (normalized.isBlank()) {

            throw new IllegalArgumentException(
                    "Object name must not be blank");

        }

        if (normalized.contains("../")
                || normalized.equals("..")) {

            throw new IllegalArgumentException(
                    "Object name must not contain parent traversal");

        }

        return normalized;

    }

    private void validateExpiration(
            Duration expiration) {

        if (expiration == null
                || expiration.isZero()
                || expiration.isNegative()) {

            throw new IllegalArgumentException(
                    "Expiration must be positive");

        }

    }

    private StorageException storageException(
            String message,
            String objectName,
            Exception cause) {

        return new StorageException(
                message + ": " + objectName,
                objectName,
                cause);

    }

}
