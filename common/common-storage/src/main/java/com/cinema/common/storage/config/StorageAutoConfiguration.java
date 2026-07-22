package com.cinema.common.storage.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import com.cinema.common.storage.minio.MinioStorageService;
import com.cinema.common.storage.properties.StorageProperties;
import com.cinema.common.storage.service.StorageService;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;

@AutoConfiguration
@ConditionalOnClass(MinioClient.class)
@EnableConfigurationProperties(StorageProperties.class)
@ConditionalOnProperty(prefix = "cinema.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient(
            StorageProperties properties) {

        validateProperties(
                properties);

        return MinioClient.builder()
                .endpoint(
                        properties.getEndpoint())
                .credentials(
                        properties.getAccessKey(),
                        properties.getSecretKey())
                .build();

    }

    @Bean
    @ConditionalOnMissingBean
    public StorageService storageService(
            MinioClient minioClient,
            StorageProperties properties) {

        return new MinioStorageService(
                minioClient,
                properties);

    }

    @Bean
    @ConditionalOnProperty(prefix = "cinema.storage", name = "create-bucket", havingValue = "true", matchIfMissing = true)
    public ApplicationRunner storageBucketInitializer(
            MinioClient minioClient,
            StorageProperties properties) {

        return arguments -> {

            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(
                                    properties.getBucket())
                            .build());

            if (!exists) {

                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(
                                        properties.getBucket())
                                .build());

            }

        };

    }

    private void validateProperties(
            StorageProperties properties) {

        requireText(
                properties.getEndpoint(),
                "cinema.storage.endpoint");

        requireText(
                properties.getAccessKey(),
                "cinema.storage.access-key");

        requireText(
                properties.getSecretKey(),
                "cinema.storage.secret-key");

        requireText(
                properties.getBucket(),
                "cinema.storage.bucket");

    }

    private void requireText(
            String value,
            String propertyName) {

        if (!StringUtils.hasText(
                value)) {

            throw new IllegalStateException(
                    propertyName
                            + " must be configured");

        }

    }

}
