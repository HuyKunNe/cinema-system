package com.cinema.common.storage.minio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cinema.common.storage.exception.StorageException;
import com.cinema.common.storage.model.StorageUploadRequest;
import com.cinema.common.storage.model.StorageUploadResult;
import com.cinema.common.storage.properties.StorageProperties;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.messages.ErrorResponse;
import io.minio.errors.ErrorResponseException;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

class MinioStorageServiceTest {

    private MinioClient minioClient;

    private StorageProperties properties;

    private MinioStorageService storageService;

    @BeforeEach
    void setUp() {

        minioClient = mock(
                MinioClient.class);

        properties = new StorageProperties();

        properties.setBucket(
                "cinema");

        properties.setPresignedUrlExpiration(
                Duration.ofMinutes(10));

        storageService = new MinioStorageService(
                minioClient,
                properties);

    }

    @Test
    void shouldUploadObject()
            throws Exception {

        ObjectWriteResponse response = new ObjectWriteResponse(
                Headers.of(),
                "cinema",
                null,
                "posters/movie.jpg",
                "etag-123",
                null);

        when(
                minioClient.putObject(
                        any(PutObjectArgs.class)))
                .thenReturn(
                        response);

        StorageUploadRequest request = new StorageUploadRequest(
                "posters/movie.jpg",
                new ByteArrayInputStream(
                        new byte[] { 1, 2, 3 }),
                3,
                "image/jpeg",
                Map.of(
                        "movie-id",
                        "movie-1"));

        StorageUploadResult result = storageService.upload(
                request);

        assertEquals(
                "cinema",
                result.bucket());

        assertEquals(
                "posters/movie.jpg",
                result.objectName());

        assertEquals(
                "etag-123",
                result.etag());

        verify(
                minioClient)
                .putObject(
                        any(PutObjectArgs.class));

    }

    @Test
    void shouldDeleteObject()
            throws Exception {

        storageService.delete(
                "posters/movie.jpg");

        verify(
                minioClient)
                .removeObject(
                        any(RemoveObjectArgs.class));

    }

    @Test
    void shouldReturnTrueWhenObjectExists()
            throws Exception {

        when(
                minioClient.statObject(
                        any(StatObjectArgs.class)))
                .thenReturn(
                        mock(
                                io.minio.StatObjectResponse.class));

        assertTrue(
                storageService.exists(
                        "posters/movie.jpg"));

    }

    @Test
    void shouldReturnFalseWhenObjectDoesNotExist()
            throws Exception {

        ErrorResponse errorResponse = new ErrorResponse(
                "NoSuchKey",
                "Object does not exist",
                "cinema",
                "posters/missing.jpg",
                null,
                null,
                null);

        Response response = new Response.Builder()
                .request(
                        new Request.Builder()
                                .url(
                                        HttpUrl.get(
                                                "http://localhost"))
                                .build())
                .protocol(
                        Protocol.HTTP_1_1)
                .code(
                        404)
                .message(
                        "Not Found")
                .build();

        ErrorResponseException exception = new ErrorResponseException(
                errorResponse,
                response,
                null);

        when(
                minioClient.statObject(
                        any(StatObjectArgs.class)))
                .thenThrow(
                        exception);

        assertFalse(
                storageService.exists(
                        "posters/missing.jpg"));

    }

    @Test
    void shouldCreatePresignedDownloadUrl()
            throws Exception {

        when(
                minioClient.getPresignedObjectUrl(
                        any(
                                GetPresignedObjectUrlArgs.class)))
                .thenReturn(
                        "http://localhost/download");

        String result = storageService.createPresignedDownloadUrl(
                "posters/movie.jpg");

        assertEquals(
                "http://localhost/download",
                result);

    }

    @Test
    void shouldRejectInvalidObjectName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.delete(
                        "../secret.txt"));

    }

    @Test
    void shouldWrapStorageFailure()
            throws Exception {

        when(
                minioClient.putObject(
                        any(PutObjectArgs.class)))
                .thenThrow(
                        new IllegalStateException(
                                "MinIO unavailable"));

        StorageUploadRequest request = StorageUploadRequest.of(
                "posters/movie.jpg",
                new ByteArrayInputStream(
                        new byte[] { 1 }),
                1,
                "image/jpeg");

        assertThrows(
                StorageException.class,
                () -> storageService.upload(
                        request));

    }

}
