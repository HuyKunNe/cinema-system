package com.cinema.booking.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class MutableTestClock extends Clock {

    private final AtomicReference<Instant> instant;

    private final ZoneId zone;

    public MutableTestClock(Instant initialInstant, ZoneId zone) {

        this.instant = new AtomicReference<>(Objects.requireNonNull(initialInstant));

        this.zone = Objects.requireNonNull(zone);
    }

    @Override
    public ZoneId getZone() {

        return zone;
    }

    @Override
    public Clock withZone(ZoneId requestedZone) {

        return new MutableTestClock(instant(), requestedZone);
    }

    @Override
    public Instant instant() {

        return instant.get();
    }

    public void set(Instant newInstant) {

        instant.set(Objects.requireNonNull(newInstant));
    }

    public void advance(Duration duration) {

        instant.updateAndGet(current -> current.plus(duration));
    }
}
