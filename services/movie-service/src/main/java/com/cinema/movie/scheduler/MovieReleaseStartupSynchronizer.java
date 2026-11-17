package com.cinema.movie.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "cinema.movie.release-lifecycle",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class MovieReleaseStartupSynchronizer {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(MovieReleaseStartupSynchronizer.class);
    private final MovieReleaseLifecycleScheduler scheduler;
    private final boolean synchronizeOnStartup;

    public MovieReleaseStartupSynchronizer(
            MovieReleaseLifecycleScheduler scheduler,
            @Value("${cinema.movie.release-lifecycle.synchronize-on-startup:true}")
                    boolean synchronizeOnStartup) {
        this.scheduler = scheduler;
        this.synchronizeOnStartup = synchronizeOnStartup;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeReleasedMovies() {
        if (!synchronizeOnStartup) return;
        try {
            scheduler.promoteReleasedMovies();
        } catch (RuntimeException exception) {
            // Preserve service availability; the midnight job remains scheduled.
            LOGGER.error(
                    "Movie release startup synchronization failed; next attempt is scheduled at"
                        + " midnight",
                    exception);
        }
    }
}
