package com.cinema.movie.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
        prefix = "cinema.movie.release-lifecycle",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class MovieReleaseSchedulingConfiguration {}
