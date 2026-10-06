package com.cinema.movie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Component
@Validated
@ConfigurationProperties(prefix = "cinema.movie.hero")
public class MovieHeroProperties {

    @Min(1)
    @Max(10)
    private int defaultLimit = 4;

    @Min(1)
    @Max(10)
    private int maxLimit = 10;

    @Min(1)
    @Max(100)
    private int maxMovieIds = 100;

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }

    public int getMaxMovieIds() {
        return maxMovieIds;
    }

    public void setMaxMovieIds(int maxMovieIds) {
        this.maxMovieIds = maxMovieIds;
    }

    @AssertTrue(message = "default-limit must not exceed max-limit")
    public boolean isLimitConfigurationValid() {
        return defaultLimit <= maxLimit;
    }
}
