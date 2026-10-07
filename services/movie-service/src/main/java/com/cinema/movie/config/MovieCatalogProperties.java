package com.cinema.movie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Component
@Validated
@ConfigurationProperties(prefix = "cinema.movie.catalog")
public class MovieCatalogProperties {

    @Min(1)
    private int defaultSize = 8;

    @Min(1)
    private int maxSize = 100;

    @Min(1)
    @Max(100)
    private int maxMovieIds = 100;

    public int getDefaultSize() {
        return defaultSize;
    }

    public void setDefaultSize(int defaultSize) {
        this.defaultSize = defaultSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getMaxMovieIds() {
        return maxMovieIds;
    }

    public void setMaxMovieIds(int maxMovieIds) {
        this.maxMovieIds = maxMovieIds;
    }

    @AssertTrue(message = "default-size must not exceed max-size")
    public boolean isSizeConfigurationValid() {
        return defaultSize <= maxSize;
    }
}
