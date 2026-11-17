package com.cinema.movie.scheduler;

import com.cinema.movie.service.MovieReleaseLifecycleService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
@ConditionalOnProperty(
        prefix = "cinema.movie.release-lifecycle",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class MovieReleaseLifecycleScheduler {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(MovieReleaseLifecycleScheduler.class);
    private final MovieReleaseLifecycleService service;
    private final Clock clock;
    private final ZoneId zone;

    public MovieReleaseLifecycleScheduler(
            MovieReleaseLifecycleService service,
            Clock clock,
            @Value("${cinema.movie.release-lifecycle.zone:Asia/Ho_Chi_Minh}") String zone) {
        this.service = service;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
    }

    @Scheduled(
            cron = "0 0 0 * * *",
            zone = "${cinema.movie.release-lifecycle.zone:Asia/Ho_Chi_Minh}")
    public void promoteReleasedMovies() {
        OffsetDateTime now = OffsetDateTime.now(clock).atZoneSameInstant(zone).toOffsetDateTime();
        int changed = service.promoteReleasedMovies(now.toLocalDate(), now);
        LOGGER.info(
                "Movie release lifecycle completed: date={}, zone={}, updated={}",
                now.toLocalDate(),
                zone,
                changed);
    }
}
