package com.cinema.common.storage.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.cinema.common.storage.service.StorageService;

import io.minio.MinioClient;

class StorageAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(
                            StorageAutoConfiguration.class));

    @Test
    void shouldCreateStorageBeans() {

        contextRunner
                .withPropertyValues(
                        "cinema.storage.enabled=true",
                        "cinema.storage.endpoint=http://localhost:9000",
                        "cinema.storage.access-key=minio",
                        "cinema.storage.secret-key=minio123",
                        "cinema.storage.bucket=cinema",
                        "cinema.storage.create-bucket=false")
                .run(
                        context -> {

                            assertThat(context)
                                    .hasSingleBean(
                                            MinioClient.class);

                            assertThat(context)
                                    .hasSingleBean(
                                            StorageService.class);

                        });

    }

    @Test
    void shouldNotCreateBeansWhenDisabled() {

        contextRunner
                .withPropertyValues(
                        "cinema.storage.enabled=false")
                .run(
                        context -> {

                            assertThat(context)
                                    .doesNotHaveBean(
                                            MinioClient.class);

                            assertThat(context)
                                    .doesNotHaveBean(
                                            StorageService.class);

                        });

    }

    @Test
    void shouldFailWhenRequiredConfigurationIsMissing() {

        contextRunner
                .withPropertyValues(
                        "cinema.storage.enabled=true")
                .run(
                        context -> {

                            assertThat(context)
                                    .hasFailed();

                        });

    }

}
