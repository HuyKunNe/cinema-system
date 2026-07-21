package com.cinema.common.core.time;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

@Component
public class SystemTimeProvider implements TimeProvider {

    @Override
    public OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

}
