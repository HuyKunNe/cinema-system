package com.cinema.common.core.id;

import java.util.UUID;

import com.github.f4b6a3.uuid.UuidCreator;

public final class UuidGenerator {

    private UuidGenerator() {
    }

    public static UUID next() {
        return UuidCreator.getTimeOrderedEpoch();
    }

}
