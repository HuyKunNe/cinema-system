package com.cinema.common.storage.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cinema.storage")
public class StorageProperties {

    private boolean enabled = true;

    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucket;

    private boolean createBucket = true;

    private Duration presignedUrlExpiration = Duration.ofMinutes(15);

    public boolean isEnabled() {

        return enabled;

    }

    public void setEnabled(
            boolean enabled) {

        this.enabled = enabled;

    }

    public String getEndpoint() {

        return endpoint;

    }

    public void setEndpoint(
            String endpoint) {

        this.endpoint = endpoint;

    }

    public String getAccessKey() {

        return accessKey;

    }

    public void setAccessKey(
            String accessKey) {

        this.accessKey = accessKey;

    }

    public String getSecretKey() {

        return secretKey;

    }

    public void setSecretKey(
            String secretKey) {

        this.secretKey = secretKey;

    }

    public String getBucket() {

        return bucket;

    }

    public void setBucket(
            String bucket) {

        this.bucket = bucket;

    }

    public boolean isCreateBucket() {

        return createBucket;

    }

    public void setCreateBucket(
            boolean createBucket) {

        this.createBucket = createBucket;

    }

    public Duration getPresignedUrlExpiration() {

        return presignedUrlExpiration;

    }

    public void setPresignedUrlExpiration(
            Duration presignedUrlExpiration) {

        this.presignedUrlExpiration = presignedUrlExpiration;

    }

}
